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
public final class ArchiveFragment_MembersInjector implements MembersInjector<ArchiveFragment> {
  private final Provider<PhoneRepository> phoneRepositoryProvider;

  private final Provider<AdminLogRepository> adminLogRepositoryProvider;

  public ArchiveFragment_MembersInjector(Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    this.phoneRepositoryProvider = phoneRepositoryProvider;
    this.adminLogRepositoryProvider = adminLogRepositoryProvider;
  }

  public static MembersInjector<ArchiveFragment> create(
      Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    return new ArchiveFragment_MembersInjector(phoneRepositoryProvider, adminLogRepositoryProvider);
  }

  @Override
  public void injectMembers(ArchiveFragment instance) {
    injectPhoneRepository(instance, phoneRepositoryProvider.get());
    injectAdminLogRepository(instance, adminLogRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.ArchiveFragment.phoneRepository")
  public static void injectPhoneRepository(ArchiveFragment instance,
      PhoneRepository phoneRepository) {
    instance.phoneRepository = phoneRepository;
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.ArchiveFragment.adminLogRepository")
  public static void injectAdminLogRepository(ArchiveFragment instance,
      AdminLogRepository adminLogRepository) {
    instance.adminLogRepository = adminLogRepository;
  }
}
