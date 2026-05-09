package com.smartselect.ui.admin;

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
public final class AdminOrdersFragment_MembersInjector implements MembersInjector<AdminOrdersFragment> {
  private final Provider<OrderRepository> orderRepositoryProvider;

  public AdminOrdersFragment_MembersInjector(Provider<OrderRepository> orderRepositoryProvider) {
    this.orderRepositoryProvider = orderRepositoryProvider;
  }

  public static MembersInjector<AdminOrdersFragment> create(
      Provider<OrderRepository> orderRepositoryProvider) {
    return new AdminOrdersFragment_MembersInjector(orderRepositoryProvider);
  }

  @Override
  public void injectMembers(AdminOrdersFragment instance) {
    injectOrderRepository(instance, orderRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminOrdersFragment.orderRepository")
  public static void injectOrderRepository(AdminOrdersFragment instance,
      OrderRepository orderRepository) {
    instance.orderRepository = orderRepository;
  }
}
