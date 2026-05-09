package com.smartselect.ui.admin;

import com.smartselect.data.repository.AdminLogRepository;
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
public final class AdminPhonesFragment_MembersInjector implements MembersInjector<AdminPhonesFragment> {
  private final Provider<PhoneRepository> phoneRepositoryProvider;

  private final Provider<AdminLogRepository> adminLogRepositoryProvider;

  public AdminPhonesFragment_MembersInjector(Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    this.phoneRepositoryProvider = phoneRepositoryProvider;
    this.adminLogRepositoryProvider = adminLogRepositoryProvider;
  }

  public static MembersInjector<AdminPhonesFragment> create(
      Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    return new AdminPhonesFragment_MembersInjector(phoneRepositoryProvider, adminLogRepositoryProvider);
  }

  @Override
  public void injectMembers(AdminPhonesFragment instance) {
    injectPhoneRepository(instance, phoneRepositoryProvider.get());
    injectAdminLogRepository(instance, adminLogRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminPhonesFragment.phoneRepository")
  public static void injectPhoneRepository(AdminPhonesFragment instance,
      PhoneRepository phoneRepository) {
    instance.phoneRepository = phoneRepository;
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminPhonesFragment.adminLogRepository")
  public static void injectAdminLogRepository(AdminPhonesFragment instance,
      AdminLogRepository adminLogRepository) {
    instance.adminLogRepository = adminLogRepository;
  }
}
