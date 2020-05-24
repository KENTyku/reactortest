package ru.kentyku.reactortest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
class ReactortestApplicationTests {

	@Test
	void createFluxTest() {
		Flux<Integer> ints = Flux.range(1, 3);
		ints.subscribe(i -> System.out.println(i));
	}

	@Test
    void createFluxWithErrorTest(){
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
    void checkMap(){
        Flux<Cat> cats=Flux.just(new Cat("Barsik",1),
                new Cat("Vasia",3));
        Flux<String> names=cats.map(cat->cat.getName());

        StepVerifier.create(names)
                .expectNext("Barsik", "Vasia")
                .expectComplete().verify();
        names.subscribe(i->System.out.println(i));
    }
}
