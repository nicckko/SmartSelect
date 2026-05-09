package com.smartselect.ui.admin;

import com.smartselect.data.repository.AdminLogRepository;
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

  private final Provider<AdminLogRepository> adminLogRepositoryProvider;

  public AdminOrdersFragment_MembersInjector(Provider<OrderRepository> orderRepositoryProvider,
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    this.orderRepositoryProvider = orderRepositoryProvider;
    this.adminLogRepositoryProvider = adminLogRepositoryProvider;
  }

  public static MembersInjector<AdminOrdersFragment> create(
      Provider<OrderRepository> orderRepositoryProvider,
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    return new AdminOrdersFragment_MembersInjector(orderRepositoryProvider, adminLogRepositoryProvider);
  }

  @Override
  public void injectMembers(AdminOrdersFragment instance) {
    injectOrderRepository(instance, orderRepositoryProvider.get());
    injectAdminLogRepository(instance, adminLogRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminOrdersFragment.orderRepository")
  public static void injectOrderRepository(AdminOrdersFragment instance,
      OrderRepository orderRepository) {
    instance.orderRepository = orderRepository;
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminOrdersFragment.adminLogRepository")
  public static void injectAdminLogRepository(AdminOrdersFragment instance,
      AdminLogRepository adminLogRepository) {
    instance.adminLogRepository = adminLogRepository;
  }
}
