package mnicky.utils.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(doNotUseGetters = true)
public class Result<SUCCESS, FAILURE> implements Serializable {

    private static final long serialVersionUID = 1;

    /* TODO:

     - add Results.lazy() ?
     - add .toStream() similar to Optional.stream() from Java 9 ?
     - add .orElseThrow throws NoSuchElementException (or some better suited exception) ?

     - https://github.com/michaelbull/kotlin-result
     - https://doc.rust-lang.org/std/result/enum.Result.html

     - add proper unit tests

    */

    private final SUCCESS success;

    private final FAILURE failure;


    /* === static factories === */

    public static <SUCCESS, FAILURE> Result<SUCCESS, FAILURE> success(SUCCESS success) {
        Objects.requireNonNull(success, "success must not be null");
        return new Result<>(success, null);
    }

    public static <SUCCESS, FAILURE> Result<SUCCESS, FAILURE> failure(FAILURE failure) {
        Objects.requireNonNull(failure, "failure must not be null");
        return new Result<>(null, failure);
    }

    public static <SUCCESS, FAILURE> Result<SUCCESS, FAILURE> ofNullable(SUCCESS nullable, FAILURE ifNull) {
        return ofNullable(nullable, () -> ifNull);
    }

    public static <SUCCESS, FAILURE> Result<SUCCESS, FAILURE> ofNullable(SUCCESS nullable, Supplier<? extends FAILURE> ifNull) {
        return nullable != null ? success(nullable) : failure(ifNull.get());
    }

    @SuppressWarnings("unchecked") //TODO: are the casts correct?
    public static <SUCCESS, FAILURE extends Exception> Result<SUCCESS, FAILURE> ofThrowable(Callable<? extends SUCCESS> action) {
        try {
            return (Result<SUCCESS, FAILURE>) ofNullable(action.call(),
                    () -> new NullPointerException("action returned null"));
        } catch (Exception e) {
            return (Result<SUCCESS, FAILURE>) failure(e);
        }
    }

    public static <SUCCESS, FAILURE> Result<SUCCESS, FAILURE> ofThrowable(Callable<? extends SUCCESS> action,
                                                                          Function<? super Exception, ? extends FAILURE> mapException)
    {
        try {
            return ofNullable(action.call(),
                    () -> mapException.apply(new NullPointerException("action returned null")));
        } catch (Exception e) {
            return failure(mapException.apply(e));
        }
    }

    public static <SUCCESS, FAILURE> Result<SUCCESS, FAILURE> ofOptional(Optional<? extends SUCCESS> optional, FAILURE ifEmpty) {
        return ofOptional(optional, () -> ifEmpty);
    }

    public static <SUCCESS, FAILURE> Result<SUCCESS, FAILURE> ofOptional(Optional<? extends SUCCESS> optional, Supplier<? extends FAILURE> ifEmpty) {
        return optional.<Result<SUCCESS, FAILURE>>map(Result::success).orElseGet(() -> failure(ifEmpty.get()));
    }

    public static <SUCCESS, FAILURE> Result<List<SUCCESS>, List<FAILURE>> all(Iterable<Result<SUCCESS, FAILURE>> results) {
        return iterate(results, (s, f) -> f.isEmpty() ? success(s) : failure(f));
    }

    public static <SUCCESS, FAILURE> Result<List<SUCCESS>, List<FAILURE>> some(Iterable<Result<SUCCESS, FAILURE>> results) {
        return iterate(results, (s, f) -> s.isEmpty() ? failure(f) : success(s));
    }

    public static <SUCCESS, FAILURE> Result<List<SUCCESS>, List<FAILURE>> iterate(Iterable<Result<SUCCESS, FAILURE>> results,
                                                                                  BiFunction<List<SUCCESS>, List<FAILURE>, Result<List<SUCCESS>, List<FAILURE>>> toResult)
    {
        final List<SUCCESS> successes = new ArrayList<>();
        final List<FAILURE> failures = new ArrayList<>();
        results.forEach(r -> r.ifSuccess(successes::add).ifFailure(failures::add));
        return toResult.apply(successes, failures);
    }


    /* === getters === */

    public SUCCESS getSuccess() throws IllegalStateException {
        if (isSuccess()) return success;
        else throw new IllegalStateException("getSuccess() cannot be called on a failure");
    }

    public FAILURE getFailure() throws IllegalStateException {
        if (isFailure()) return failure;
        else throw new IllegalStateException("getFailure() cannot be called on a success");
    }


    /* === equals & hashCode === */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.isFailure() || getClass() != o.getClass()) return false;
        final Result<?, ?> other = (Result<?, ?>) o;
        return other.isSuccess() && Objects.equals(success, other.success);
    }

    @Override
    public int hashCode() {
        return this.isSuccess() ? Objects.hashCode(success) : System.identityHashCode(this);
    }


    /* === methods === */

    public boolean isSuccess() {
        return success != null;
    }

    public boolean isFailure() {
        return failure != null;
    }
    
    // TODO: add this one?
    //
    // public Result<SUCCESS, FAILURE> doEffect(Consumer<Result<? super SUCCESS2, ? super FAILURE>> consumer) {
    //     consumer.accept(this);
    //     return this;
    // }
    
    // TODO: add ifSuccessOrElse() like Optional.ifPresentOrElse() from java 9 ?

    public Result<SUCCESS, FAILURE> ifSuccess(Consumer<? super SUCCESS> consumer) {
        if (isSuccess()) consumer.accept(getSuccess());
        return this;
    }

    public Result<SUCCESS, FAILURE> ifFailure(Consumer<? super FAILURE> consumer) {
        if (isFailure()) consumer.accept(getFailure());
        return this;
    }

    //TODO: filter() methods don't allow to change FAILURE type. Is this correct?

    public Result<SUCCESS, FAILURE> filter(Predicate<? super SUCCESS> predicate, FAILURE failure) {
        return filter(predicate, () -> failure);
    }


    public Result<SUCCESS, FAILURE> filter(Predicate<? super SUCCESS> predicate, Supplier<? extends FAILURE> failureSupplier) {
        if (isSuccess()) return predicate.test(getSuccess()) ? this : failure(failureSupplier.get());
        else return failure(getFailure());
    }


    //TODO: these two and() methods reuse failure from the previous call - does it make sense?

    @SuppressWarnings("unchecked") // safe covariant cast
    public <SUCCESS2> Result<SUCCESS2, FAILURE> and(Function<? super SUCCESS, Result<? extends SUCCESS2, ? extends FAILURE>> mapper) {
        if (isSuccess()) return (Result<SUCCESS2, FAILURE>) mapper.apply(getSuccess());
        else return failure(getFailure());
    }

    public <SUCCESS2> Result<SUCCESS2, FAILURE> andResultOf(Function<? super SUCCESS, ? extends SUCCESS2> mapper) {
        if (isSuccess()) return success(mapper.apply(getSuccess()));
        else return failure(getFailure());
    }

    public <FAILURE2> Result<SUCCESS, FAILURE2> mapFailure(Function<? super FAILURE, ? extends FAILURE2> failureMapper) {
        if (isSuccess()) return success(getSuccess());
        else return failure(failureMapper.apply(getFailure()));
    }

    public <SUCCESS2, FAILURE2> Result<SUCCESS2, FAILURE2> mapBoth(Function<? super SUCCESS, ? extends SUCCESS2> successMapper,
                                                       Function<? super FAILURE, ? extends FAILURE2> failureMapper)
    {
        if (isSuccess()) return success(successMapper.apply(getSuccess()));
        else return failure(failureMapper.apply(getFailure()));
    }

    public Result<SUCCESS, FAILURE> or(Result<? extends SUCCESS, ? extends FAILURE> other) {
        return or(() -> other);
    }

    @SuppressWarnings("unchecked") // safe covariant cast
    public Result<SUCCESS, FAILURE> or(Supplier<Result<? extends SUCCESS, ? extends FAILURE>> other) {
        if (isSuccess()) return this;
        else return (Result<SUCCESS, FAILURE>) other.get();
    }


    // terminating //

    public SUCCESS orElse(SUCCESS other) {
        return orElse(() -> other);
    }

    public SUCCESS orElse(Supplier<? extends SUCCESS> other) {
        if (isSuccess()) return getSuccess();
        else return other.get();
    }

    public SUCCESS orElseNull() {
        if (isSuccess()) return getSuccess();
        else return null;
    }

    public <E extends Throwable> SUCCESS orElseThrow(Function<? super FAILURE, ? extends E> exceptionSupplier) throws E {
        if (isSuccess()) return getSuccess();
        //TODO: is this cast correct?
        else throw (E) exceptionSupplier.apply(getFailure());
    }

    public Set<SUCCESS> toSet() {
        if (isSuccess()) return Collections.singleton(getSuccess());
        else return Collections.emptySet();
    }

    public Optional<SUCCESS> toOptional() {
        if (isSuccess()) return Optional.of(getSuccess());
        else return Optional.empty();
    }

}
