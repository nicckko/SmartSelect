package com.smartselect.ui.orders;

import com.google.firebase.auth.FirebaseAuth;
import com.smartselect.data.repository.OrderRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class CheckoutFragment_MembersInjector implements MembersInjector<CheckoutFragment> {
  private final Provider<OrderRepository> orderRepositoryProvider;

  private final Provider<FirebaseAuth> authProvider;

  public CheckoutFragment_MembersInjector(Provider<OrderRepository> orderRepositoryProvider,
      Provider<FirebaseAuth> authProvider) {
    this.orderRepositoryProvider = orderRepositoryProvider;
    this.authProvider = authProvider;
  }

  public static MembersInjector<CheckoutFragment> create(
      Provider<OrderRepository> orderRepositoryProvider, Provider<FirebaseAuth> authProvider) {
    return new CheckoutFragment_MembersInjector(orderRepositoryProvider, authProvider);
  }

  @Override
  public void injectMembers(CheckoutFragment instance) {
    injectOrderRepository(instance, orderRepositoryProvider.get());
    injectAuth(instance, authProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.orders.CheckoutFragment.orderRepository")
  public static void injectOrderRepository(CheckoutFragment instance,
      OrderRepository orderRepository) {
    instance.orderRepository = orderRepository;
  }

  @InjectedFieldSignature("com.smartselect.ui.orders.CheckoutFragment.auth")
  public static void injectAuth(CheckoutFragment instance, FirebaseAuth auth) {
    instance.auth = auth;
  }
}
