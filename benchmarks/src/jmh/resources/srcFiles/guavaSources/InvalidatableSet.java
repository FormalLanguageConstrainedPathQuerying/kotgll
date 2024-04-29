package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingSet;
import java.util.Set;

/**
 * A subclass of `ForwardingSet` that throws `IllegalStateException` on invocation of any method
 * (except `hashCode` and `equals`) if the provided `Supplier` returns false.
 */
@ElementTypesAreNonnullByDefault
final class InvalidatableSet<E> extends ForwardingSet<E> {
  private final Supplier<Boolean> validator;
  private final Set<E> delegate;
  private final Supplier<String> errorMessage;

  public static final <E> InvalidatableSet<E> of(
      Set<E> delegate, Supplier<Boolean> validator, Supplier<String> errorMessage) {
    return new InvalidatableSet<>(
        checkNotNull(delegate), checkNotNull(validator), checkNotNull(errorMessage));
  }

  @Override
  protected Set<E> delegate() {
    validate();
    return delegate;
  }

  private InvalidatableSet(
      Set<E> delegate, Supplier<Boolean> validator, Supplier<String> errorMessage) {
    this.delegate = delegate;
    this.validator = validator;
    this.errorMessage = errorMessage;
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  private void validate() {
    if (!validator.get()) {
      throw new IllegalStateException(errorMessage.get());
    }
  }
}
