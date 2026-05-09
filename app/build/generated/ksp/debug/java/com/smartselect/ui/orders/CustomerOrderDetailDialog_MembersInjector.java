package com.smartselect.ui.orders;

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
public final class CustomerOrderDetailDialog_MembersInjector implements MembersInjector<CustomerOrderDetailDialog> {
  private final Provider<OrderRepository> orderRepositoryProvider;

  public CustomerOrderDetailDialog_MembersInjector(
      Provider<OrderRepository> orderRepositoryProvider) {
    this.orderRepositoryProvider = orderRepositoryProvider;
  }

  public static MembersInjector<CustomerOrderDetailDialog> create(
      Provider<OrderRepository> orderRepositoryProvider) {
    return new CustomerOrderDetailDialog_MembersInjector(orderRepositoryProvider);
  }

  @Override
  public void injectMembers(CustomerOrderDetailDialog instance) {
    injectOrderRepository(instance, orderRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.orders.CustomerOrderDetailDialog.orderRepository")
  public static void injectOrderRepository(CustomerOrderDetailDialog instance,
      OrderRepository orderRepository) {
    instance.orderRepository = orderRepository;
  }
}
