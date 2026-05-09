package com.smartselect.ui.profile;

import com.google.firebase.storage.FirebaseStorage;
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
public final class EditProfileDialog_MembersInjector implements MembersInjector<EditProfileDialog> {
  private final Provider<FirebaseStorage> storageProvider;

  public EditProfileDialog_MembersInjector(Provider<FirebaseStorage> storageProvider) {
    this.storageProvider = storageProvider;
  }

  public static MembersInjector<EditProfileDialog> create(
      Provider<FirebaseStorage> storageProvider) {
    return new EditProfileDialog_MembersInjector(storageProvider);
  }

  @Override
  public void injectMembers(EditProfileDialog instance) {
    injectStorage(instance, storageProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.profile.EditProfileDialog.storage")
  public static void injectStorage(EditProfileDialog instance, FirebaseStorage storage) {
    instance.storage = storage;
  }
}
