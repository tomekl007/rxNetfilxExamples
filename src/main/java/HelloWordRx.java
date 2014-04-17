import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * @author Tomasz Lelek
 * @since 2014-04-17
 */
public class HelloWordRx {

    public static void main(String[] args) throws MalformedURLException {
        hello("a", "b", "c");

        Observable<String> o = Observable.from("a", "b", "c");
        Observable<String> o2 = Observable.just("one object");

        // To see output:
        Observable<String> observable = customObservableBlocking(() -> {
            System.out.println("in runnable");
            throw new Exception();
        });


        observable.subscribe(it -> System.out.println(it));

        System.out.println("sync");

        // To see output:
        customObservableNonBlocking().subscribe(it -> System.out.println(it));
        System.out.println("async");


        fetchWikipediaArticleAsynchronously("Tiger", "Elephant")
                .subscribe(it -> System.out.println(it));


        customObservableNonBlocking().skip(10).take(5)
                .map(stringValue -> stringValue + "_form")
                .subscribe(it -> System.out.println(it));

    }

    public static void hello(String... names) {
        Observable.from(names).subscribe(s -> {
            System.out.println("Hello " + s + "!");
        });
    }

    public static Observable<String> customObservableBlocking(Callable func) {
        return Observable.create((Subscriber<? super String> subscriber) -> {
            for (int i = 0; i < 50; i++) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        func.call();
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                    subscriber.onNext("value_" + i);

                }


            }
            subscriber.onCompleted();
        }).doOnError(throwable -> System.out.println("error " + throwable))
                .onErrorReturn(throwable -> "onErrorReturn " + throwable)
                .onErrorResumeNext(throwable -> {
                    System.out.println("onErrorResumeNext:  " + throwable.getMessage());
                    return Observable.error(throwable);
                });


    }


    /**
     * This example shows a custom Observable that does not block
     * when subscribed to as it spawns a separate thread.
     */
    public static Observable<String> customObservableNonBlocking() {
        return Observable.create((Subscriber<? super String> subscriber) -> {
            /**
             * This 'call' method will be invoked with the Observable is subscribed to.
             *
             * It spawns a thread to do it asynchronously.
             */

            // For simplicity this example uses a Thread instead of an ExecutorService/ThreadPool
            final Thread t = new Thread(() -> {
                for (int i = 0; i < 75; i++) {
                    if (subscriber.isUnsubscribed()) {
                        return;
                    }
                    subscriber.onNext("value_" + i);
                }
                // after sending all values we complete the sequence
                subscriber.onCompleted();
            });
            t.start();
        }
        );
    }


    /**
     * Fetch a list of Wikipedia articles asynchronously.
     */
    public static Observable<String> fetchWikipediaArticleAsynchronously(String... wikipediaArticleNames) throws MalformedURLException {
        return Observable.create((Subscriber<? super String> subscriber) -> {
            final Thread t = new Thread(() -> {
                for (String articleName : wikipediaArticleNames) {
                    if (subscriber.isUnsubscribed()) {
                        return;
                    }
                    try {
                        subscriber.onNext(new URL("http://en.wikipedia.org/wiki/" + articleName).openConnection().getContentEncoding());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // after sending all values we complete the sequence
                subscriber.onCompleted();
            });
            t.start();
        });
    }


}
