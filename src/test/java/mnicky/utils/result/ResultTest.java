package mnicky.utils.result;

import java.util.Objects;

import org.junit.Test;

/**
 * Heavily inspired from https://github.com/michaelbull/kotlin-result
 */
public class ResultTest {


  @Test
  public void testNulable() {

//    System.out.println(
//        handleInput("this is not string")
//            .andResultOfNullable(this::parseOrNull, "invalid parser input")
//    );
//
//
//    System.out.println(
//        handleInput("this is not string")
//            .ifSuccess(r -> System.out.println("SUCC!"))
//            .ifFailure(r -> System.out.println("FAIL!"))
//            .orElse(handleInputOrThrow("another try"))
//    );


//        Result<Integer, ? extends Exception> res = parseResult("lslsls")
//                .ifSuccess(s -> System.out.println("we have success: " + s))
//                .ifFailure(f -> System.out.println("we have failure: " + f))
//                .or(parseResult2("fdfdf"));
//
//        Result<Number, String> in = Result.success(0);
//
//        in.or(Result.success((float) 0.1));
//
//        Optional<Integer> i = Optional.of(5);
//
//        Result<? extends Number, String> r = Result.ofOptional(i, "empty");

    Result<Integer, ? extends Throwable> res = Result.ofThrowable(() -> parseOrThrowChecked("test"));
    System.out.println(res);
    System.out.println(Result.ofThrowable(() -> {throw new IllegalStateException("1");}));
    System.out.println(Result.ofThrowable(() -> {throw new RuntimeException("1");}));
    System.out.println(Result.ofThrowable(() -> {throw new Exception("1");}));

    Result<Integer, String> rex = Result.ofThrowable(
        () -> parseOrThrowChecked("test"),
        Throwable::getMessage);


  }


  private Result<String, String> handleInput(String input) {
    return Result.ofNullable(input, "input can't be null");
  }

  private String handleInputOrThrow(String input) {
    return Objects.requireNonNull(input, "input can't be null");
  }

  private Integer parseOrNull(String stringInteger) {
    try {
      return Integer.parseInt(stringInteger);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Result<Integer, IllegalArgumentException> parseResult(String stringInteger) {
    try {
      return Result.success(Integer.parseInt(stringInteger));
    } catch (NumberFormatException e) {
      return Result.failure(new IllegalArgumentException(e));
    }
  }

  private Result<Integer, NumberFormatException> parseResult2(String stringInteger) {
    try {
      return Result.success(Integer.parseInt(stringInteger));
    } catch (NumberFormatException e) {
      return Result.failure(e);
    }
  }

  private Integer parseOrThrow(String stringInteger) {
    return Integer.parseInt(stringInteger);
  }

  private Integer parseOrThrowChecked(String stringInteger) throws Exception {
    try {
      return Integer.parseInt(stringInteger);
    } catch (NumberFormatException e) {
      throw new Exception(e);
    }
  }


}