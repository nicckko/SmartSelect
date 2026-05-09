package com.smartselect.ui.admin;

import com.smartselect.data.repository.AdminLogRepository;
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
public final class ActivityLogsFragment_MembersInjector implements MembersInjector<ActivityLogsFragment> {
  private final Provider<AdminLogRepository> adminLogRepositoryProvider;

  public ActivityLogsFragment_MembersInjector(
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    this.adminLogRepositoryProvider = adminLogRepositoryProvider;
  }

  public static MembersInjector<ActivityLogsFragment> create(
      Provider<AdminLogRepository> adminLogRepositoryProvider) {
    return new ActivityLogsFragment_MembersInjector(adminLogRepositoryProvider);
  }

  @Override
  public void injectMembers(ActivityLogsFragment instance) {
    injectAdminLogRepository(instance, adminLogRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartselect.ui.admin.ActivityLogsFragment.adminLogRepository")
  public static void injectAdminLogRepository(ActivityLogsFragment instance,
      AdminLogRepository adminLogRepository) {
    instance.adminLogRepository = adminLogRepository;
  }
}
