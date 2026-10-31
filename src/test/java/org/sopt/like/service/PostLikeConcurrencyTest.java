package org.sopt.like.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.like.exception.AlreadyLikedException;
import org.sopt.like.repository.PostLikeRepository;
import org.sopt.member.domain.Member;
import org.sopt.member.repository.MemberRepository;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PostLikeConcurrencyTest {
    private static final int THREADS = 10;

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long postId;
    private List<Long> likerIds;

    @BeforeEach
    void setUp() {
        clear();
        Member author = memberRepository.save(Member.create("작성자"));
        postId = postRepository.save(Post.create("제목", "본문", author, Category.FREE)).getId();
        likerIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            likerIds.add(memberRepository.save(Member.create("회원" + i)).getId());
        }
    }

    @AfterEach
    void tearDown() {
        clear();
    }

    private void clear() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("서로 다른 회원 10명이 같은 글에 동시에 좋아요를 눌러도 좋아요 수가 정확하다")
    void concurrentLikesByDifferentMembers() throws Exception {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Long likerId : likerIds) {
            tasks.add(() -> {
                postLikeService.like(postId, likerId);
                return null;
            });
        }

        List<Throwable> failures = runConcurrently(tasks);

        assertTrue(failures.isEmpty(), "실패한 요청이 있습니다: " + failures);
        assertEquals(THREADS, postLikeRepository.countByPostId(postId));
        assertEquals(THREADS, postRepository.findById(postId).orElseThrow().getLikeCount());
    }

    @Test
    @DisplayName("같은 회원이 같은 글에 동시에 10번 눌러도 한 번만 성공하고 나머지는 중복으로 거절된다")
    void concurrentLikesBySameMember() throws Exception {
        Long likerId = likerIds.get(0);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> {
                postLikeService.like(postId, likerId);
                return null;
            });
        }

        List<Throwable> failures = runConcurrently(tasks);

        assertEquals(THREADS - 1, failures.size());
        assertTrue(failures.stream().allMatch(AlreadyLikedException.class::isInstance),
                "중복 외의 실패가 있습니다: " + failures);
        assertEquals(1, postLikeRepository.countByPostId(postId));
        assertEquals(1, postRepository.findById(postId).orElseThrow().getLikeCount());
    }

    @Test
    @DisplayName("10명이 동시에 좋아요를 취소하면 좋아요 수가 정확히 0이 된다")
    void concurrentUnlikes() throws Exception {
        for (Long likerId : likerIds) {
            postLikeService.like(postId, likerId);
        }
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Long likerId : likerIds) {
            tasks.add(() -> {
                postLikeService.unlike(postId, likerId);
                return null;
            });
        }

        List<Throwable> failures = runConcurrently(tasks);

        assertTrue(failures.isEmpty(), "실패한 요청이 있습니다: " + failures);
        assertEquals(0, postLikeRepository.countByPostId(postId));
        assertEquals(0, postRepository.findById(postId).orElseThrow().getLikeCount());
    }

    @Test
    @DisplayName("좋아요와 취소가 섞여 동시에 들어와도 좋아요 수와 좋아요 행 수가 일치한다")
    void concurrentMixedLikesAndUnlikes() throws Exception {
        List<Long> alreadyLiked = likerIds.subList(0, THREADS / 2);
        List<Long> willLike = likerIds.subList(THREADS / 2, THREADS);
        for (Long likerId : alreadyLiked) {
            postLikeService.like(postId, likerId);
        }
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Long likerId : alreadyLiked) {
            tasks.add(() -> {
                postLikeService.unlike(postId, likerId);
                return null;
            });
        }
        for (Long likerId : willLike) {
            tasks.add(() -> {
                postLikeService.like(postId, likerId);
                return null;
            });
        }

        List<Throwable> failures = runConcurrently(tasks);

        assertTrue(failures.isEmpty(), "실패한 요청이 있습니다: " + failures);
        long rows = postLikeRepository.countByPostId(postId);
        assertEquals(THREADS / 2, rows);
        assertEquals(rows, postRepository.findById(postId).orElseThrow().getLikeCount());
    }

    private List<Throwable> runConcurrently(List<Callable<Void>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        try {
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();

            List<Throwable> failures = new ArrayList<>();
            for (Future<Void> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            return failures;
        } finally {
            executor.shutdownNow();
        }
    }
}
