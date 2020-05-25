package ru.kentyku.reactortest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

@SpringBootTest
class ReactortestApplicationTests {

    @Test
    void createFluxTest() {
        Flux<Integer> ints = Flux.range(1, 3);
        ints.subscribe(i -> System.out.println(i));
    }

    @Test
    void createFluxWithErrorTest() {
        Flux<Integer> ints = Flux.range(1, 4)
                .map(i -> {
                    if (i <= 3) {
                        return i;
                    }
                    throw new RuntimeException("Got to 4");
                });
        ints.subscribe(
                i -> System.out.println(i), error -> System.err.println("Error: " + error)
        );
    }

    @Test
    void checkMap() {
        Flux<Cat> cats = Flux.just(new Cat("Barsik", 1),
                new Cat("Vasia", 3));
        Flux<String> names = cats.map(cat -> cat.getName());

        StepVerifier.create(names)
                .expectNext("Barsik", "Vasia")
                .expectComplete().verify();
        names.subscribe(i -> System.out.println(i));
    }

    @Test
    void checkDefer() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 1));
        Mono<String> name = cat.map(c -> c.getName());

        Mono<String> defMono = Mono.defer(() -> name.map(n -> n + "Test"));

        StepVerifier.create(defMono)
                .expectNext("BarsikTest")
                .expectComplete().verify();
        defMono.subscribe(i -> System.out.println(i));
    }

    @Test
    void checkFromRunnableAndThen() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 1));
        Mono<String> name = cat.map(c -> c.getName());
        Mono<Void> emptyMono = Mono
                .fromRunnable(() -> System.out.println("Выполним что-то без " +
                        "входных параметров и результата"));//fromRunnable- создает пустой моно после выполнения кода
        emptyMono
                .then(name)//then - испускает новый элемент,завершив текущий
                .subscribe(n -> System.out.println(n));

        StepVerifier.create(emptyMono)
                .expectComplete().verify();

        StepVerifier.create(name)
                .expectNext("Barsik")
                .expectComplete().verify();

    }

    @Test
    void handleError() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 1));
        Mono<String> name = cat.map(c -> c.getName());

        Mono<String> resultMono = name
                .map(n -> n.concat("Test"))
                .map(n -> {
                    if (Objects.equals(n, "BarsikTest")) {
                        throw new RuntimeException("Mono failed");
                    }
                    return n;
                })
                .onErrorReturn("BarsikAfterError");


        resultMono.subscribe(n -> System.out.println(n));

        StepVerifier.create(name)
                .expectNext("Barsik")
                .expectComplete();

        StepVerifier.create(resultMono)
                .expectNext("BarsikAfterError")
                .expectComplete()
                .verify();

    }

    @Test
    void throwError() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 1));
        Mono<String> name = cat.map(c -> c.getName());

        Mono<String> resultMono = name
                .map(n -> n.concat("Test"))
                .map(n -> {
                    if (Objects.equals(n, "BarsikTest")) {
                        throw new RuntimeException("Mono failed");
                    }
                    return n;
                });

        StepVerifier.create(name)
                .expectNext("Barsik")
                .expectComplete();

        StepVerifier.create(resultMono)
//                .expectError(RuntimeException.class)
                .expectErrorMessage("Mono failed")
                .verify();
    }

}
