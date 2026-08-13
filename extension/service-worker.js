const MALGO_API_URL =
  "http://localhost:8080/api/v1/analyses";


chrome.runtime.onMessage.addListener(
  (message, sender, sendResponse) => {

    if (message?.type !== "MALGO_ANALYZE") {
      return;
    }

    handleAnalyze(message.payload)
      .then((data) => {

        sendResponse({
          ok: true,
          data: data
        });

      })
      .catch((error) => {

        console.error(
          "[Malgo] API 호출 실패:",
          error
        );

        sendResponse({
          ok: false,
          message: error.message
        });

      });


    // 비동기 sendResponse를 사용하기 위해 필요
    return true;
  }
);


async function handleAnalyze(payload) {

  const stored =
    await chrome.storage.local.get(
      "accessToken"
    );


  const headers = {
    "Content-Type": "application/json"
  };


  // 나중에 로그인/JWT 연결할 때 사용
  if (stored.accessToken) {

    headers["Authorization"] =
      `Bearer ${stored.accessToken}`;

  }


  const response = await fetch(
    MALGO_API_URL,
    {
      method: "POST",

      headers: headers,

      body: JSON.stringify(payload)
    }
  );


  if (!response.ok) {

    const body =
      await response.text();

    throw new Error(
      `${response.status} ${body}`
    );
  }


  return await response.json();
}
