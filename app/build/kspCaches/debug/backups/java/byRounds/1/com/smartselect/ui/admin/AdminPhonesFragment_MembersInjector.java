package com.smartselect.ui.admin;

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

  public AdminPhonesFragment_MembersInjector(Provider<PhoneRepository> phoneRepositoryProvider) {
    this.phoneRepositoryProvider = phoneRepositoryProvider;
  }

  public static MembersInjector<AdminPhonesFragment> create(
      Provider<PhoneRepository> phoneRepositoryProvider) {
    return new AdminPhonesFragment_MembersInjector(phoneRepositoryProvider);
  }

  @Override
  public void injectMembers(AdminPhonesFragment instance) {
    injectPhoneRepository(instance, phoneRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AdminPhonesFragment.phoneRepository")
  public static void injectPhoneRepository(AdminPhonesFragment instance,
      PhoneRepository phoneRepository) {
    instance.phoneRepository = phoneRepository;
  }
}
