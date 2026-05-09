package com.smartselect.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PhoneRepository_Factory implements Factory<PhoneRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public PhoneRepository_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public PhoneRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static PhoneRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider) {
    return new PhoneRepository_Factory(firestoreProvider);
  }

  public static PhoneRepository newInstance(FirebaseFirestore firestore) {
    return new PhoneRepository(firestore);
  }
}
