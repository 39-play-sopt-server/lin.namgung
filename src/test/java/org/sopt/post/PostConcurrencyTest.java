package org.sopt.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.post.domain.Category;
import org.sopt.post.domain.Post;
import org.sopt.post.dto.response.PostResponse;
import org.sopt.post.repository.PostRepository;
import org.sopt.post.service.PostService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostConcurrencyTest {
    private static final int THREADS = 32;
    private static final int REPEAT = 200;

    @Test
    @DisplayName("여러 사용자가 동시에 게시글을 작성해도 id가 중복되지 않고 하나도 유실되지 않는다")
    void concurrentCreate() throws Exception {
        PostRepository repository = new PostRepository();
        Set<Long> ids = ConcurrentHashMap.newKeySet();

        runConcurrently(() -> {
            for (int i = 0; i < REPEAT; i++) {
                Post saved = repository.save(Post.create("제목", "본문", "작성자", Category.FREE));
                ids.add(saved.getId());
            }
        });

        assertEquals(THREADS * REPEAT, ids.size());
        assertEquals(THREADS * REPEAT, repository.count(null));
    }

    @Test
    @DisplayName("같은 게시글을 동시에 조회해도 조회수가 유실되지 않는다")
    void concurrentView() throws Exception {
        PostRepository repository = new PostRepository();
        PostService postService = new PostService(repository);
        PostResponse created = postService.registerPost("제목", "본문", "작성자", Category.FREE);

        runConcurrently(() -> {
            for (int i = 0; i < REPEAT; i++) {
                postService.getPost(created.id());
            }
        });

        assertEquals(THREADS * REPEAT, repository.findById(created.id()).getViewCount());
    }

    @Test
    @DisplayName("제목과 본문을 동시에 수정해도 항상 같은 수정 요청의 값 쌍으로 유지된다")
    void concurrentUpdateKeepsPairConsistent() throws Exception {
        PostRepository repository = new PostRepository();
        PostService postService = new PostService(repository);
        PostResponse created = postService.registerPost("제목", "본문", "작성자", Category.FREE);
        Post post = repository.findById(created.id());
        List<String> broken = new ArrayList<>();

        runConcurrently(() -> {
            for (int i = 0; i < REPEAT; i++) {
                String value = Thread.currentThread().getName() + "-" + i;
                postService.changePost(created.id(), value, value);
                PostResponse response = postService.getPost(created.id());
                if (response.title() == null || response.content() == null) {
                    synchronized (broken) {
                        broken.add("null 값이 조회되었습니다.");
                    }
                }
            }
        });

        assertTrue(broken.isEmpty());
        assertNotNull(post.getTitle());
        assertEquals(post.getTitle(), post.getContent());
    }

    private void runConcurrently(Runnable task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                task.run();
            }));
        }

        ready.await();
        start.countDown();
        try {
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
