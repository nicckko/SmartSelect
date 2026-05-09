package com.smartselect.viewmodel;

import com.smartselect.data.repository.PhoneRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class PhoneViewModel_Factory implements Factory<PhoneViewModel> {
  private final Provider<PhoneRepository> phoneRepositoryProvider;

  public PhoneViewModel_Factory(Provider<PhoneRepository> phoneRepositoryProvider) {
    this.phoneRepositoryProvider = phoneRepositoryProvider;
  }

  @Override
  public PhoneViewModel get() {
    return newInstance(phoneRepositoryProvider.get());
  }

  public static PhoneViewModel_Factory create(Provider<PhoneRepository> phoneRepositoryProvider) {
    return new PhoneViewModel_Factory(phoneRepositoryProvider);
  }

  public static PhoneViewModel newInstance(PhoneRepository phoneRepository) {
    return new PhoneViewModel(phoneRepository);
  }
}
