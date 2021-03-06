/*
 * Copyright 2015 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, without warranties or
 * conditions of any kind, EITHER EXPRESS OR IMPLIED.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.vmware.photon.controller.apife.auth.fetcher;

import com.vmware.photon.controller.api.PersistentDisk;
import com.vmware.photon.controller.apife.auth.TransactionAuthorizationObject;
import com.vmware.photon.controller.apife.backends.DiskBackend;
import com.vmware.photon.controller.apife.exceptions.external.DiskNotFoundException;

import com.google.common.collect.ImmutableSet;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Set;

/**
 * Tests {@link DiskSecurityGroupFetcher}.
 */
public class DiskSecurityGroupFetcherTest {

  private DiskBackend backend;
  private ProjectSecurityGroupFetcher projectFetcher;

  private DiskSecurityGroupFetcher fetcher;

  /**
   * Dummy method so that intellj recognizes this as a test class.
   */
  @Test(enabled = false)
  private void dummy() {

  }

  /**
   * Tests the fetchSecurityGroup method.
   */
  public class FetchSecurityGroupsTest {

    private TransactionAuthorizationObject authorizationObject;

    @BeforeMethod
    private void setUp() {
      backend = mock(DiskBackend.class);
      projectFetcher = mock(ProjectSecurityGroupFetcher.class);
      fetcher = new DiskSecurityGroupFetcher(backend, projectFetcher);

      authorizationObject = new TransactionAuthorizationObject(
          TransactionAuthorizationObject.Kind.DISK,
          TransactionAuthorizationObject.Strategy.PARENT,
          "id");
    }

    /**
     * Tests that only transaction authorization objects of kind
     * DISK are accepted.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "authorizationObject must be of 'kind' DISK.")
    public void testWrongTransactionAuthorizationObjectKind() {
      fetcher.fetchSecurityGroups(new TransactionAuthorizationObject(TransactionAuthorizationObject.Kind.NONE));
    }

    @Test
    public void testInvalidId() throws Throwable {
      doThrow(new DiskNotFoundException("id")).when(backend).toApiRepresentation("id");

      Set<String> groups = fetcher.fetchSecurityGroups(authorizationObject);
      assertThat(groups.size(), is(0));
    }

    @Test
    public void testParentWithoutSecurityGroups() throws Throwable {
      authorizationObject.setStrategy(TransactionAuthorizationObject.Strategy.PARENT);

      PersistentDisk disk = new PersistentDisk();
      disk.setProjectId("project-id");
      doReturn(disk).when(backend).toApiRepresentation("id");

      ArgumentCaptor<TransactionAuthorizationObject> captor =
          ArgumentCaptor.forClass(TransactionAuthorizationObject.class);
      doReturn(ImmutableSet.of()).when(projectFetcher).fetchSecurityGroups(captor.capture());

      Set<String> groups = fetcher.fetchSecurityGroups(authorizationObject);
      assertThat(groups.size(), is(0));
      assertThat(captor.getValue().getId(), is("project-id"));
    }

    @Test
    public void testParentWithSecurityGroups() throws Throwable {
      authorizationObject.setStrategy(TransactionAuthorizationObject.Strategy.PARENT);

      PersistentDisk disk = new PersistentDisk();
      disk.setProjectId("project-id");
      doReturn(disk).when(backend).toApiRepresentation("id");

      ArgumentCaptor<TransactionAuthorizationObject> captor =
          ArgumentCaptor.forClass(TransactionAuthorizationObject.class);
      doReturn(ImmutableSet.of("SG1", "SG2"))
          .when(projectFetcher).fetchSecurityGroups(captor.capture());

      Set<String> groups = fetcher.fetchSecurityGroups(authorizationObject);
      assertThat(groups.size(), is(2));
      assertThat(groups, contains("SG1", "SG2"));
      assertThat(captor.getValue().getId(), is("project-id"));
    }
  }
}
