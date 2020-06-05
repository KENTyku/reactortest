package ru.kentyku.reactortest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void checkThatCatchIsNotWorkForMono() {
        Exception exception = assertThrows(RuntimeException.class, () -> method2().block());
        System.out.println(exception.getMessage());
    }

    private Mono<Void> method2() {
        try {
            return submitOrder();
        }
        catch (RuntimeException e){
            System.out.println("Catch error");
            throw e;
        }
    }

    private Mono<Void> submitOrder(){
        return Mono.fromRunnable( ()-> {
            throw new RuntimeException("Нет сообщения о том что отработал " +
            "блок catch(Catch error)");
        }
        );
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

    @Test
    void noCatchError() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 7));
        Mono<Integer> age = cat.map(c -> c.getAge());
        Mono<Integer> resultMono;
        try {
            //если мы пытаемся обработать ошибку в моно или флакс стандартным образом через try catch то блок catch
            //не выполниться (в выводе консоли не увидите сообщение Catch error).
            // Тут необходимо использовать реактивные методы doOnError, onErrorResume и т.п.
            resultMono = age
                    .map(n -> n + 3)
                    .map(n -> n / 0);
        } catch (ArithmeticException e) {
            System.out.println("Catch error");
            throw e;
        }

        StepVerifier.create(age)
                .expectNext(7)
                .expectComplete();

        StepVerifier.create(resultMono)
                .expectError(ArithmeticException.class)
                .verify();

        Exception exception = assertThrows(UnsupportedOperationException.class,
                () -> resultMono.subscribe(a -> System.out.println(a)));
        System.out.println(exception.getMessage());
    }

    @Test
    void checkDoOnSuccess() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 7));
        Mono<Integer> age = cat.map(c -> c.getAge());

        Mono<Integer> resultMono = age
                .map(n -> n + 3);

        StepVerifier.create(age)
                .expectNext(7)
                .expectComplete();

        StepVerifier.create(resultMono)
                .expectNext(10)
                .expectComplete()
                .verify();
    }

    @Test
    void checkSeveralMethodsTest() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 7));//age=7
        Mono<Integer> age = cat.map(c -> c.getAge());

        Mono<Integer> resultMono = age
                .flatMap(n -> {

                    return cat
                            .map(c -> {
                                c.setAge(c.getAge() + 3);//age=10
                                return c;
                            })
                            .doOnNext(c -> System.out.println("doOnNext 1 " + (c.getAge() + 10)))
                            .doOnNext(c->c.setAge(c.getAge() + 40)) //age=50
                            .doOnNext(c -> System.out.println("doOnNext 2 " + (c.getAge() + 10000)));

                })
                .map(c->c.getAge()+1000);

//        StepVerifier.create(age)
//                .expectNext(7)
//                .expectComplete();
//
//        StepVerifier.create(resultMono)
//                .expectNext(1010)
//                .expectComplete()
//                .verify();

        resultMono.subscribe(n-> System.out.println(n));
    }
}
