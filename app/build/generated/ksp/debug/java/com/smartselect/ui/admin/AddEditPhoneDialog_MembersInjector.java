package com.smartselect.ui.admin;

import com.google.firebase.storage.FirebaseStorage;
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
public final class AddEditPhoneDialog_MembersInjector implements MembersInjector<AddEditPhoneDialog> {
  private final Provider<PhoneRepository> phoneRepositoryProvider;

  private final Provider<FirebaseStorage> storageProvider;

  public AddEditPhoneDialog_MembersInjector(Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<FirebaseStorage> storageProvider) {
    this.phoneRepositoryProvider = phoneRepositoryProvider;
    this.storageProvider = storageProvider;
  }

  public static MembersInjector<AddEditPhoneDialog> create(
      Provider<PhoneRepository> phoneRepositoryProvider,
      Provider<FirebaseStorage> storageProvider) {
    return new AddEditPhoneDialog_MembersInjector(phoneRepositoryProvider, storageProvider);
  }

  @Override
  public void injectMembers(AddEditPhoneDialog instance) {
    injectPhoneRepository(instance, phoneRepositoryProvider.get());
    injectStorage(instance, storageProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AddEditPhoneDialog.phoneRepository")
  public static void injectPhoneRepository(AddEditPhoneDialog instance,
      PhoneRepository phoneRepository) {
    instance.phoneRepository = phoneRepository;
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.AddEditPhoneDialog.storage")
  public static void injectStorage(AddEditPhoneDialog instance, FirebaseStorage storage) {
    instance.storage = storage;
  }
}
