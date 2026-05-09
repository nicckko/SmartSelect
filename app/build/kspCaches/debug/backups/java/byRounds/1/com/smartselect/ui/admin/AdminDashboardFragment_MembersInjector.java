package com.smartselect.ui.admin;

import com.smartselect.data.repository.OrderRepository;
import com.smartselect.data.repository.PhoneRepository;
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
public final class AdminDashboardFragment_MembersInjector implements MembersInjector<AdminDashboardFragment> {
  private final Provider<PhoneRepository> phoneRepositoryProvider;

  private final Provider<OrderRepository> orderRepositoryProvider;

  public AdminDashboardFragment_MembersInjector(Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<OrderRepository> orderRepositoryProvider) {
    this.phoneRepositoryProvider = phoneRepositoryProvider;
    this.orderRepositoryProvider = orderRepositoryProvider;
  }

  public static MembersInjector<AdminDashboardFragment> create(
      Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<OrderRepository> orderRepositoryProvider) {
    return new AdminDashboardFragment_MembersInjector(phoneRepositoryProvider, orderRepositoryProvider);
  }

  @Override
  public void injectMembers(AdminDashboardFragment instance) {
    injectPhoneRepository(instance, phoneRepositoryProvider.get());
    injectOrderRepository(instance, orderRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminDashboardFragment.phoneRepository")
  public static void injectPhoneRepository(AdminDashboardFragment instance,
      PhoneRepository phoneRepository) {
    instance.phoneRepository = phoneRepository;
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminDashboardFragment.orderRepository")
  public static void injectOrderRepository(AdminDashboardFragment instance,
      OrderRepository orderRepository) {
    instance.orderRepository = orderRepository;
  }
}
