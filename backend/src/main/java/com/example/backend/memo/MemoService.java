package com.example.backend.memo;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemoService {

    private final MemoRepository memoRepository;

    public MemoService(MemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    @Transactional
    public MemoResponse create(MemoCreateRequest request) {
        Memo memo = Memo.create(request.normalizedTitle(), request.normalizedContent());
        return MemoResponse.from(memoRepository.save(memo));
    }

    @Transactional(readOnly = true)
    public List<MemoResponse> findAll() {
        return memoRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(MemoResponse::from)
                .toList();
    }
}
