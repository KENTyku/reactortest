package ru.kentyku.reactortest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

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
        } catch (RuntimeException e) {
            System.out.println("Catch error");
            throw e;
        }
    }

    private Mono<Void> submitOrder() {
        return Mono.fromRunnable(() -> {
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
                .doOnSuccess(n -> System.out.println(n + 3));

        StepVerifier.create(age)
                .expectNext(7)
                .expectComplete();

        StepVerifier.create(resultMono)
                .expectNext(7)
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
                            .doOnNext(c -> c.setAge(c.getAge() + 40)) //age=50
                            .doOnNext(c -> System.out.println("doOnNext 2 " + (c.getAge() + 10000)));

                })
                .map(c -> c.getAge() + 1000);

//        StepVerifier.create(age)
//                .expectNext(7)
//                .expectComplete();
//
//        StepVerifier.create(resultMono)
//                .expectNext(1010)
//                .expectComplete()
//                .verify();

        resultMono.subscribe(n -> System.out.println(n));
    }


    @Test
    void checkFilter() {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 7));
        Mono<Integer> age = cat.map(c -> c.getAge());

        Mono<Integer> resultMono = age
                .map(n -> n + 3)
                .filter(n -> n != 10)
                .map(n -> Pair.of("test", n))
                .map(p -> p.getValue());


        Mono<Integer> fs = Mono.defer(() -> resultMono);

        fs.subscribe(n -> System.out.println("============ " + n));
        System.out.println("end");

//        StepVerifier.create(age)
//                .expectNext(7)
//                .expectComplete();
//
//        StepVerifier.create(resultMono)
//                .expectNext(7)
//                .expectComplete()
//                .verify();
    }

    @Test
    void generateTest() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3 * state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux.subscribe(s -> System.out.println(s));
    }

    @Test
    void generateObjectTest() {
        Flux<String> flux = Flux.generate(
                AtomicLong::new,
                (state, sink) -> {
                    long i = state.getAndDecrement();
                    sink.next("3 x " + i + " = " + 3 * i);
                    if (i == -10) sink.complete();
                    return state;
                });
        flux.subscribe(s -> System.out.println(s));
    }

    @Test
    void generateObjectConsumerTest() {
        Flux<String> flux = Flux.generate(
                AtomicLong::new,
                (state, sink) -> {
                    long i = state.getAndIncrement();
                    sink.next("3 x " + i + " = " + 3 * i);
                    if (i == 10) sink.complete();
                    return state;
                }, (state) ->
                {
                    state.set(0);
                    System.out.println(state);
                });

        flux.subscribe(s -> System.out.println(s));
    }

    @Test
    void generateFluxFromProcessorTest() {
        final EmitterProcessor<Cat> updates = EmitterProcessor.create();
        final FluxSink<Cat> sink = updates.sink();//get FluxSing from processor
        Cat cat = new Cat("Barsik", 1);
        sink.next(cat);//it's emitting cat to Flux
        cat = new Cat("Barsik2", 2);
        sink.next(cat);//it's emitting cat to Flux
        Flux<Cat> fluxCat = updates;

        fluxCat.subscribe(c -> System.out.println(c.getName()));
    }

    @Test
    void sum() {

        String[] array = {
                "9b65e049-6ebb-4897-be74-3726472cac8c",
                "15556823-2674-4435-a880-d3c36a56e51b",
                "29391347-68cc-4c4b-a303-292b53518b61",
                "8a2271ef-5a8f-4042-b31c-c16be0ee4c32",
                "e6a1ae6d-c534-452a-bcba-cb5f44e14849",
                "c7de5138-8900-404e-83c7-aa03e1b17cf4",
                "661c949f-3416-427a-8cef-9ef93360af5e",
                "6f7c3f11-eb31-45b3-bc1c-4bf45acd34f2",
                "5dc47907-0efa-43ec-904d-7bf23dc4ce2d",
                "1738096c-9d97-446d-b606-1cfd4da16bc6",
                "905cb19e-1a86-4eb5-a712-9f2c9ec7fbc1",
                "9cf50626-624b-473f-a6b8-dfcb2ed89711",
                "e4a4addc-1920-474a-b144-b23d023ae5bf",
                "0d5ede35-1d23-4eb1-8c7d-b900c90432d1",
                "d1ae7f8d-f98c-41fd-9a1f-b6559a2ac3bd",
                "af1a1c6d-9ce3-4866-a7a1-5ab0a631e7c8",
                "c5e71380-18a6-45b3-a9ee-441e4283e2cc",
                "092408da-c467-4412-ac36-27c8fd385505",
                "47ba7a4e-4e3e-4908-80ae-9655ab6d989e",
                "e5296bcd-6524-45b7-aff5-1eb306cd93ed",
                "13a0e439-ee86-4713-9929-a17f483ab998",
                "cab3e269-910b-4e9e-8620-31bb74ac6518",
                "e85cb730-be00-4d74-80c6-0983352a54ac",
                "bcebce13-6094-4234-b1b6-6bfc63c4d557",
                "87243d9c-bc22-43ac-8667-41c98f606ddb",
                "a6e900a6-0b10-4a96-8e83-2cf81e99e0ed",
                "703585b4-73b5-40e0-b23f-1c08e8c5b099",
                "77b6d92e-59cc-4d13-a85d-b0aa0080a9d6",
                "173523d4-aef1-4ea0-b1e1-15fbd7a9c683",
                "66a66783-e369-482e-8c5c-00e09e2d8d67",
                "7c881ed9-5e86-4ff1-8435-ba775577747f",
                "cf8071b0-8249-4dc0-b11e-3286772544fd",
                "21141525-3f9d-48a2-8143-a4ee96bba432",
                "75a362e1-44ce-48b6-a294-e5f7520167dc",
                "3039f07f-3791-4906-9b79-42b1eeab4fc5",
                "2ef79d77-3fd2-4083-8878-32d4f7629596",
                "e0ac6cf6-8eec-4db6-9794-c8c90ad31944",
                "0ec789ae-3eb0-4130-8a9d-27c7eab03126",
                "aa7c8282-28eb-460e-a4eb-12adb7a09bf8",
                "a3436e6d-cee8-460f-bc0f-7aad488eb48e",
                "4dbfffda-d35c-4308-ae11-9af63202ddd1",
                "8240f5cf-443e-487d-a4e7-e37f27c4292e",
                "c9dd5319-b033-41d8-aecc-84c8998cdb8a",
                "3e401fa0-099e-45bd-9215-7cf97991cd8e",
                "c95c8621-a40b-4737-a32c-17a0b4459841",
                "8473ff2f-cf15-4c8c-b39a-444d594ed59f",
                "31566022-1411-4e86-be52-08fc9951762b",
                "b613719a-2586-4a26-9301-45b539e7cb58",
                "5fdafe7e-e176-46e0-ac3f-c69d4584e9af",
                "6a6f701d-e8cc-4851-b977-ee59c1ac1bbb",
                "71cc54e4-50cf-4ddc-b1dc-f022011845cc",
                "389b21f9-fbe8-4118-abde-054ef5e5ddc1",
                "395cd712-cda4-41cb-b09b-a2977a503965",
                "70731efc-0936-4e7c-ac66-329e0f72d35a",
                "7616c00a-1be7-4939-a458-0ec41cdc05bd",
                "ced3857a-716c-45db-8d35-f4a1c08bf891",
                "4d87c56e-77bb-4d7c-badc-b548a29bea84",
                "53afe4b3-b115-40f0-92af-e49aad6058d3",
                "834cd281-67cc-45fb-ba57-270d8236aaf6",
                "a7dae1fe-ef3a-4fcb-ad83-4958591b81bb",
                "6186dbb6-6569-4d74-8e8c-1dca9de45982",
                "79c4c031-aa66-4d6b-b1e6-c33d1f456351",
                "dea88d49-13cc-4f4c-bcc3-52318831f542",
                "4282695f-fe60-490c-aec3-62bb426f807f",
                "07126ab9-e77d-4932-ae43-9bb8be2e5191",
                "939e1d86-5b3d-4253-b6b9-5ff2d57b4d30",
                "7b5920d0-8c58-46df-9cee-c69647514b8b",
                "d6ee872a-6619-4169-a833-1ecceab098b0",
                "04d52c46-ee65-4a76-8a41-58d0546d4dff",
                "598460e1-0895-45d1-9da4-54d59f97283d",
                "eeca7588-1820-4a14-bf04-44b2ba51f2e8",
                "41e5dbcf-7c2f-4fbe-a89d-5a62408d5da9",
                "686c090b-5c3e-4a1f-b167-8309e3e27451",
                "6c63450e-b30b-48bc-9854-ec42cef2141a",
                "d51daedb-7f05-4bb9-a4af-d7c958635156",
                "1d58dcb8-cd3d-4137-8c73-ba884ecc5b97",
                "de428262-8bd7-4e01-b929-55bcc16b5c1f",
                "a0f7f947-05f3-4c40-b6d2-9d1d9560fcc0",
                "a472745b-d474-456d-aa58-0f34b4fe639c",
                "5df380a1-f37d-433a-a21e-717309dec7bc",
                "f41e0c09-b944-4c59-abfe-d7a265f1886c",
                "e1130bc3-f8f2-4ec4-a654-82be46ba19d1",
                "70649fcf-f772-4319-98f6-9d80ef1515e8",
                "7415c515-07f9-47a5-9fdb-cd3ba2acc5f4",
                "30cd2c83-9be9-42a5-9654-89c87aa065ec",
                "b243396b-2d8d-47b4-8ff1-1a7def1474d5",
                "136151a4-9703-46e9-87ac-7e97072b3cde",
                "18fa6a0d-b4c6-4f3c-aa00-17f6f26512b5",
                "ea3084ff-5086-43cb-b283-b49e15bd38ea",
                "c747607c-b922-4297-9cfe-5d67bf927173",
                "27aa527c-2513-4160-995a-98363dc4c311",
                "e826ce23-e0f5-41e7-8583-18a608f11099",
                "71f3fd72-10ca-48b0-91ce-dedd58bc0ee9",
                "a280d60a-11e4-4022-ac53-100be15da917",
                "3f09a4e1-c612-49be-b0fa-711a924ed62b",
                "724e4277-ed02-4af4-a0f4-aa83c97b2192",
                "b102fee3-b862-43fa-8297-4dfedac1aa47",
                "d3d73354-8c29-43ce-9647-3c055c1efdf6",
                "711e74bc-1faa-48a3-935c-7c6bb1d38034",
                "bd4920c9-0057-4db0-946f-7497aad487b7",
                "8134965c-d15f-449b-b6dc-ceeb807d6a6e",
                "5f2c729d-0821-4018-9223-e6d95e7058e3",
                "879e4b0c-ed18-4a0b-aac1-56aff405e061",
                "064581a5-09d9-4621-a611-4d4ca98e3c31",
                "beeaddb6-6223-4216-8eb7-cb470a5398fb",
                "7b3ea80c-fce8-452d-9edf-ad9ec1e7d595",
                "f5c0b742-4f20-4c19-b344-31eff35cb2c4",
                "3204efd6-a08d-4bb3-9f01-d04670d23808",
                "eeb6d710-5e19-439d-b01d-a28a64efb852",
                "06ce19d5-9405-485a-b0de-38a4f718b666",
                "1eccf304-d167-457f-9fe0-733a4c1c1bf3",
                "1cf94097-9847-463e-a709-c742e54548ac",
                "d9df7c83-2dbe-4398-a409-243b7f49f32f",
                "a73b1997-ee61-4a35-adae-170c1aa5da74",
                "dffd581b-b08a-450d-b98a-a991678d09f3",
                "8f8eeb7d-9067-4b76-b0dd-d618d5f1105e",
                "fde2dfd8-b54d-4b7f-80db-90f8b833cef7",
                "6c7ff64a-8708-4d81-b19f-799ae2830123",
                "0788bf8c-af1c-4830-99a0-7047f806f0fe",
                "75bd63f0-3420-40d7-b4e1-2dbbd31ec976",
                "9430cf6f-ad7a-4e0b-bdef-dd446a124c39",
                "f4d2b0e8-9172-4ebd-b828-1e6726d6df08",
                "93010d3e-15a1-4452-a770-7fbcfb04286f",
                "bd168893-37a6-4458-9eed-4e7357dbdecf",
                "27e7506e-351c-44f3-b6d0-759e6754ffcf",
                "706f0a7c-d9a0-4918-91c9-a47eb8aa8c99",
                "ba44fd01-0155-4a9a-a83e-fdf9b88aaf4d",
                "0dd48c7b-ef94-40d2-aa3e-3cf93d1056f5",
                "d5a0bb9a-078f-4a19-8a22-19ac383205e0",
                "a62278ee-0bf5-4de7-a9b2-a6fb35d72a50",
                "6be324d1-7720-4ddc-a008-9fd421fdc482",
                "e04913b4-ee6a-4c1b-ba51-484b3d37da8b",
                "1c996ef9-f566-4c11-b615-1f13dd8e88cd",
                "373f23b0-d83e-4567-afdd-1e490179ff38",
                "93355d50-7315-4997-8d9d-4ee5318c4051",
                "cfdb1a95-372b-4fdc-b84a-e068e8d2a141",
                "d06fe40f-7366-4cef-be29-206a38fd5fea",
                "71743bc6-bb48-4877-8713-ad8404f2cdf8",
                "1f197485-63f1-4a5a-a711-1b07c1c89388",
                "23a29e8f-0532-49e3-8052-dbee14953798",
                "1dda1505-77f3-426b-9b03-6b0b9f79245b",
                "a83d700e-d2a3-40d3-92cf-d24245021f6f",
                "7bbb2fe1-c251-48aa-bba4-873e027ab622",
                "5e636078-f46b-4a0e-a823-47f80fd2d546",
                "3582c420-d98c-4889-994f-f6bae708e218",
                "9b8c62c7-75bc-4627-9a3b-f453af184d52",
                "bfb4b939-eee8-4c33-9eca-dcdae70f8dca",
                "fa96e984-1c18-4466-8f84-2e664825c127",
                "ef4e978b-ffc8-497f-8b5d-3dac0c0da7af",
                "94f046fa-f988-422b-a8be-a2b186693ae3",
                "f2a32a7d-e6df-492c-847e-0d241408540a",
                "4e694f9a-46a9-4eed-a044-b04e5d7f4eac",
                "8274ad2a-6605-4371-9567-8656a77c6731",
                "bcc37ddc-d33c-44f2-8093-dc8a4cf0d208",
                "63e6d6b5-e9cc-46bb-b8a1-f376c8bb3d32",
                "8a3fad89-82cd-4951-9985-bb02295b1dae",
                "3c8bb9d0-4f17-4f2a-bd39-39688f03cbdf",
                "4e230819-9fd4-4a45-a88e-b556e73f0a97",
                "0848d1f6-07f5-4a78-bada-7c6a2d741281",
                "2aa4ff70-8a85-4067-81a4-dc6ef847a5c1",
                "d63521a7-393b-4b93-b620-51180f322f26",
                "05486bc4-2aa2-4f43-a34d-2f6bb268fab3",
                "46a08cdf-3fdb-4cea-bb0c-139e7115a3a7",
                "c51a880a-c26a-4275-a513-f5028ccde5b1",
                "3f2c3678-3b01-4b7d-b950-bac2609ca8e0",
                "42fc19c0-88f1-4534-9cd8-9c4caa1670bf",
                "a2048a2c-5b9d-4c4c-9ee9-63c806080b6c",
                "1902248d-f37f-4591-9ea0-a8a8b7852616",
                "7b449731-192e-41ad-ad0d-2c1ea21719b6",
                "d97d71cb-9239-4978-99f2-a2249808cd1b",
                "130b3756-0e83-4b42-8c5b-d9ef3ed6c961",
                "f5522ae1-ec32-4f4c-8fc1-ce6b8d00b2ca",
                "f390d614-82e0-4e2c-98cf-4e89e4179af7",
                "55f6c0d7-4171-451e-88f4-cf0301da7241",
                "8de02216-f10b-48fe-a35f-d608a148c431",
                "60907772-a987-47c8-ba43-aaca9e5cda0f",
                "fc7f9b46-4d3a-4dfc-ad6c-1a59ace0e8c4",
                "becab3d6-1c90-4c2c-930d-639bcdfa3618",
                "f41dbfa8-17d0-4190-9296-e41ee38c8b0d",
                "0ae5e025-50b1-46cf-b2ee-df35738a542b",
                "36aad530-7854-4921-8d27-e3b9f5bad2ea",
                "d47e1e5b-49f9-43d1-b7f2-91738210a5c1",
                "6ab3ede7-f0a5-46ae-945f-209359b0c4d8",
                "66be8417-089c-492c-849b-24903e6b562a",
                "0aa2d893-13bd-473c-8563-efe74b92be5c",
                "5d5c4043-9669-4589-80b5-09d898d88669",
                "d5a4ca15-0517-4444-ad84-63a8710dd0f3",
                "a3a7c1e1-52c9-4967-bbe7-8b6763b8b5e7",
                "a50e8bc9-60a6-4e27-bbb8-ac44310ffb77",
                "36a0e326-7978-4dc8-bf71-26e03e0bc4c3",
                "5b37226a-5c41-4d7e-8897-bfe98b50b2c7",
                "ce4414d2-4511-47fa-a0a6-708379616e90",
                "976aea72-ae42-4e60-99f0-56b09ae1d8f5",
                "9bd1bee6-f334-4f54-909b-d73ee1d8e1fa",
                "51998afb-2e3f-46d4-b69d-9bc09753eca2",
                "8ed47cd6-c6c6-4756-9e72-758854acea39",
                "de190698-2cfb-45ec-b768-b16b854b6673",
                "04e19aea-b553-4128-8661-d60b0c6e1c01",
                "6ca08d7a-11f8-4db3-bbdf-3670e8f3802c",
                "648c468f-7482-4211-b29d-763bf257871a",
                "261e150d-4446-46b5-83e4-8b99b483d7f5",
                "4cde0eed-56f9-41f0-80f6-f1d1480197a5",
                "9ed1e66f-e22f-4014-ac4f-b29d6bf23787",
                "5f7f9317-f13e-4bae-b2c6-204f1f826971",
                "74d17591-11c2-4855-82d1-3275f5667b94",
                "69cd43cd-4cd5-4561-9a41-ff4436415e2d",
                "fb621a3c-72dd-466e-856d-985753e48099",
                "165a5f46-c54e-4fff-9499-478d4f78ca52",
                "b2219bb5-4a3b-4613-9e55-7997490ff5df",
                "81680582-ff4d-4172-955c-5ea7ab0a27fd",
                "25304790-b6e7-4f85-9494-f01949203d5f",
                "7fd33ad2-c70a-4278-ae07-31c11124e8a7",
                "905dc6f9-b6b6-4b3b-9026-b2de0983400f",
                "984876f4-9743-4648-999c-94d8c38240fe",
                "f356a905-6bd3-4772-9da0-eced5be813ef",
                "ef5aa1fa-92f9-4625-8276-4fc8a2b02aa6",
                "eeb78ab5-66c6-4727-a04c-ef080dddb6f0",
                "3de5b79e-b3bc-40b8-b2d6-70a44540391d",
                "778602f3-b292-4d69-b43b-bbc8518a4027",
                "9c68fc76-20e0-466e-b00c-2cb95c64cb64"

        };

        Stream.of(array).map(i -> "{\"match_phrase\": { \"transactionId\": \"" + i + "\"}},")
                .forEach(i -> System.out.println(i));
    }


    @Test
    void checkFlatMap() throws InterruptedException {
        Mono<Cat> cat = Mono.just(new Cat("Barsik", 1));

        cat.flatMap(c -> Mono.just(c.getAge()).map(a ->
                {
                    System.out.println(" 1000");
                    //здесь мог быть код вызова из другого сервиса, и если бы мы не возвращали в этом блоке Mono (как
                    // например при использовании оператора map, то дальнейший поток был бы заблокирован этим блоком
                    //на время работы сервиса. Но так так мы возвращаем в лямбде Mono(поэтому используется
                    // flatMap), то код блока не блокирующий и поток выполнения идет дальше к **, не задерживаясь на этом
                    // блоке
                    return a;
                }
                )
                        .delayElement(Duration.ofMillis(5))
        )
                //** продолжение обработки потока
                .map(a -> a)
                .delayElement(Duration.ofMillis(1))
                //когда происходит подписка на поток то последовательно начинает выполняться код сверху вниз
                // при этом дойдя до блока поток будет ждать пока выполнится блок, а затем продолжит выполнение вниз
                .subscribe((a) ->
                        System.out.println("100"));

        //задержку использовал чтобы проверить как основной ооток и поток блока flatMap зависят друг от друга
        //также чтобы корректно отрабатывала задержка необходимо чтоб программа исполнялась, пэ ниже добавлен код для
        // работы программы:
        long i = 0;
        while (i < 10000000) {
            i = i + 1;
        }
    }

}
