package com.smartselect.data.repository;

import com.google.firebase.auth.FirebaseAuth;
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
public final class OrderRepository_Factory implements Factory<OrderRepository> {
  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  public OrderRepository_Factory(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public OrderRepository get() {
    return newInstance(authProvider.get(), firestoreProvider.get());
  }

  public static OrderRepository_Factory create(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    return new OrderRepository_Factory(authProvider, firestoreProvider);
  }

  public static OrderRepository newInstance(FirebaseAuth auth, FirebaseFirestore firestore) {
    return new OrderRepository(auth, firestore);
  }
}
