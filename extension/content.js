(() => {

  const MIN_TEXT_LENGTH = 5;

  // 사용자가 타이핑을 멈춘 뒤
  // 분석까지 기다리는 시간
  const DEBOUNCE_MS = 900;


  let typingTimer = null;

  let isComposing = false;

  let malgoPopup = null;

  let requestSequence = 0;

  let applyingSuggestion = false;


  // ==============================
  // 1. 한글 입력 조합 시작
  // ==============================

  document.addEventListener(
    "compositionstart",
    (event) => {

      if (findEditable(event.target)) {
        isComposing = true;
      }

    },
    true
  );


  // ==============================
  // 2. 한글 입력 조합 종료
  // ==============================

  document.addEventListener(
    "compositionend",
    (event) => {

      isComposing = false;

      scheduleAnalysis(
        event.target
      );

    },
    true
  );


  // ==============================
  // 3. 입력 감지
  // ==============================

  document.addEventListener(
    "input",
    (event) => {

      if (
        event.isComposing ||
        isComposing ||
        applyingSuggestion
      ) {
        return;
      }


      scheduleAnalysis(
        event.target
      );

    },
    true
  );


  // ESC 누르면 팝업 닫기

  document.addEventListener(
    "keydown",
    (event) => {

      if (event.key === "Escape") {
        removePopup();
      }

    },
    true
  );


  // ==============================
  // 분석 예약
  // ==============================

  function scheduleAnalysis(target) {

    const editable =
      findEditable(target);


    if (!editable) {
      return;
    }


    clearTimeout(
      typingTimer
    );


    const text =
      getText(editable)
        .trim();


    if (
      text.length <
      MIN_TEXT_LENGTH
    ) {

      removePopup();

      return;
    }


    // 새로운 요청 번호 생성
    const currentRequest =
      ++requestSequence;


    typingTimer =
      setTimeout(() => {

        analyzeText(
          editable,
          text,
          currentRequest
        );

      }, DEBOUNCE_MS);
  }


  // ==============================
  // 입력창 찾기
  // ==============================

  function findEditable(target) {

    if (!target) {
      return null;
    }


    // textarea
    if (
      target.tagName ===
      "TEXTAREA"
    ) {
      return target;
    }


    // 일반 input
    if (
      target.tagName ===
      "INPUT"
    ) {

      const allowedTypes = [
        "text",
        "search"
      ];


      if (
        allowedTypes.includes(
          target.type
        )
      ) {
        return target;
      }
    }


    // Gmail / Slack 등의
    // contenteditable 입력창
    if (
      target.isContentEditable
    ) {
      return target;
    }


    // 내부 span 등에 이벤트가 발생했을 경우
    return target.closest?.(
      '[contenteditable="true"]'
    ) || null;
  }


  // ==============================
  // 입력창 텍스트 가져오기
  // ==============================

  function getText(element) {

    if (
      element.tagName ===
      "INPUT" ||
      element.tagName ===
      "TEXTAREA"
    ) {

      return element.value || "";
    }


    return (
      element.innerText ||
      element.textContent ||
      ""
    );
  }


  // ==============================
  // Malgo 서버에 분석 요청
  // ==============================

  async function analyzeText(
    editable,
    originalText,
    requestNumber
  ) {

    const payload = {

      text: originalText,

      targetCountry: "US",

      targetLanguage: "en",

      situation: "WORK",

      relationship: "COWORKER",

      tone: "POLITE",

      purpose:
        "WORK_COMMUNICATION"
    };


    try {

      const response =
        await chrome.runtime.sendMessage({

          type:
            "MALGO_ANALYZE",

          payload: payload
        });


      // 더 새로운 요청이 있다면
      // 오래된 응답은 무시
      if (
        requestNumber !==
        requestSequence
      ) {
        return;
      }


      // AI 처리 도중
      // 사용자가 문장을 바꿨다면 무시
      if (
        getText(editable)
          .trim()
        !== originalText
      ) {
        return;
      }


      if (!response?.ok) {

        console.error(
          "[Malgo]",
          response?.message
        );

        return;
      }


      const result =
        response.data;


      if (!result) {
        return;
      }


      // 보여줄 내용이 하나도 없으면
      // 팝업 표시하지 않음
      if (
        !result.reason &&
        !result.rewrittenText &&
        !result.translatedText
      ) {

        removePopup();

        return;
      }


      showPopup(
        editable,
        result
      );


    } catch (error) {

      console.error(
        "[Malgo] 분석 실패:",
        error
      );
    }
  }


  // ==============================
  // Malgo 팝업
  // ==============================

  function showPopup(
    editable,
    result
  ) {

    removePopup();


    const popup =
      document.createElement(
        "div"
      );


    popup.className =
      "malgo-popup";


    // ----------
    // Header
    // ----------

    const header =
      document.createElement(
        "div"
      );

    header.className =
      "malgo-header";


    const title =
      document.createElement(
        "div"
      );

    title.textContent =
      "✨ Malgo";


    const closeButton =
      document.createElement(
        "button"
      );

    closeButton.className =
      "malgo-close";

    closeButton.textContent =
      "×";


    header.appendChild(title);

    header.appendChild(
      closeButton
    );


    popup.appendChild(
      header
    );


    // ----------
    // 위험도
    // ----------

    const status =
      document.createElement(
        "div"
      );


    if (result.hasRisk) {

      status.className =
        "malgo-status malgo-risk";

      status.textContent =
        "⚠ 오해 가능성이 있어요";

    } else {

      status.className =
        "malgo-status malgo-safe";

      status.textContent =
        "✓ 표현을 분석했어요";
    }


    popup.appendChild(
      status
    );


    // ----------
    // 이유
    // ----------

    if (result.reason) {

      const reason =
        document.createElement(
          "div"
        );

      reason.className =
        "malgo-reason";

      reason.textContent =
        result.reason;


      popup.appendChild(
        reason
      );
    }


    // ----------
    // 한국어 추천
    // ----------

    if (
      result.rewrittenText
    ) {

      addSuggestionSection(
        popup,

        " 의도·맥락을 반영한 표현",

        result.rewrittenText,

        "한국어 적용",

        () => {

          replaceText(
            editable,
            result.rewrittenText
          );

          removePopup();
        }
      );
    }


    // ----------
    // 영어 추천
    // ----------

    if (
      result.translatedText
    ) {

      addSuggestionSection(
        popup,

        " 자연스러운 영어 표현",

        result.translatedText,

        "영어로 적용",

        () => {

          replaceText(
            editable,
            result.translatedText
          );

          removePopup();
        }
      );
    }


    document.body.appendChild(
      popup
    );


    closeButton.addEventListener(
      "click",
      removePopup
    );


    malgoPopup = popup;


    positionPopup(
      popup,
      editable
    );
  }


  // ==============================
  // 추천 영역 생성
  // ==============================

  function addSuggestionSection(
    popup,
    labelText,
    suggestionText,
    buttonText,
    onApply
  ) {

    const section =
      document.createElement(
        "div"
      );


    section.className =
      "malgo-section";


    const label =
      document.createElement(
        "div"
      );


    label.className =
      "malgo-label";


    label.textContent =
      labelText;


    const suggestion =
      document.createElement(
        "div"
      );


    suggestion.className =
      "malgo-suggestion";


    suggestion.textContent =
      suggestionText;


    const button =
      document.createElement(
        "button"
      );


    button.className =
      "malgo-apply";


    button.textContent =
      buttonText;


    button.addEventListener(
      "click",
      onApply
    );


    section.appendChild(
      label
    );

    section.appendChild(
      suggestion
    );

    section.appendChild(
      button
    );


    popup.appendChild(
      section
    );
  }


  // ==============================
  // 입력창 위에 팝업 배치
  // ==============================

  function positionPopup(
    popup,
    editable
  ) {

    const rect =
      editable.getBoundingClientRect();


    const popupWidth =
      360;


    const margin =
      12;


    let left =
      rect.left;


    // 화면 오른쪽 밖으로 나가지 않게
    left =
      Math.min(
        left,
        window.innerWidth -
        popupWidth -
        margin
      );


    left =
      Math.max(
        margin,
        left
      );


    popup.style.left =
      `${left}px`;


    // 팝업 높이를 얻기 위해
    // DOM에 추가된 뒤 계산
    const popupHeight =
      popup.offsetHeight;


    let top =
      rect.top -
      popupHeight -
      10;


    // 위에 공간이 없으면
    // 입력창 아래에 표시
    if (top < margin) {

      top =
        rect.bottom +
        10;
    }


    popup.style.top =
      `${top}px`;
  }


  // ==============================
  // 실제 입력창 문장 교체
  // ==============================

  function replaceText(
    element,
    newText
  ) {

    applyingSuggestion =
      true;


    // input / textarea
    if (
      element.tagName ===
      "INPUT" ||
      element.tagName ===
      "TEXTAREA"
    ) {

      const prototype =

        element.tagName ===
        "TEXTAREA"

          ? HTMLTextAreaElement
              .prototype

          : HTMLInputElement
              .prototype;


      const descriptor =
        Object.getOwnPropertyDescriptor(
          prototype,
          "value"
        );


      if (
        descriptor?.set
      ) {

        descriptor.set.call(
          element,
          newText
        );

      } else {

        element.value =
          newText;
      }


      element.dispatchEvent(
        new Event(
          "input",
          {
            bubbles: true
          }
        )
      );

    }

    // Gmail / Slack
    // contenteditable
    else {

      element.focus();


      const selection =
        window.getSelection();


      const range =
        document.createRange();


      range.selectNodeContents(
        element
      );


      selection.removeAllRanges();

      selection.addRange(
        range
      );


      const inserted =
        document.execCommand(
          "insertText",
          false,
          newText
        );


      // 혹시 insertText가 실패하면
      // fallback
      if (!inserted) {

        element.textContent =
          newText;


        element.dispatchEvent(
          new InputEvent(
            "input",
            {
              bubbles: true,

              inputType:
                "insertText",

              data:
                newText
            }
          )
        );
      }
    }


    setTimeout(
      () => {

        applyingSuggestion =
          false;

      },
      100
    );
  }


  // ==============================
  // 팝업 제거
  // ==============================

  function removePopup() {

    if (malgoPopup) {

      malgoPopup.remove();

      malgoPopup =
        null;
    }
  }

})();
