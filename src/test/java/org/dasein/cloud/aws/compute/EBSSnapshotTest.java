/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */

package org.dasein.cloud.aws.compute;

import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Jeffrey Yan on 2/1/2016.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AWSCloud.class, EBSSnapshot.class })
public class EBSSnapshotTest extends AwsTestBase {

    private EBSSnapshot ebsSnapshot;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        ebsSnapshot = new EBSSnapshot(awsCloudStub);
    }

    protected Document resource(String resourceName) throws Exception {
        return super.resource("org/dasein/cloud/aws/compute/snapshot/" + resourceName);
    }

    @Test
    public void testGetSnapshot() throws Exception {
        String snapshotId = "snap-1a2b3c4d";

        EC2Method describeSnapshotsStub = mock(EC2Method.class);
        when(describeSnapshotsStub.invoke()).thenReturn(resource("describe_snapshots.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("SnapshotId.1", snapshotId), hasEntry("Action", "DescribeSnapshots"))))
                .thenReturn(describeSnapshotsStub);

        Snapshot snapshot = ebsSnapshot.getSnapshot(snapshotId);
        assertEquals(SnapshotState.PENDING, snapshot.getCurrentState());
        assertEquals("111122223333", snapshot.getOwner());
        assertEquals("demo_snapshot", snapshot.getName());
        assertEquals("Daily Backup", snapshot.getDescription());
        assertEquals("30%", snapshot.getProgress());
        assertEquals("us-east-1", snapshot.getRegionId());
        assertEquals(15, snapshot.getSizeInGb());
        assertEquals(1452148872485l, snapshot.getSnapshotTimestamp());
        assertEquals("vol-1a2b3c4d", snapshot.getVolumeId());
        assertEquals(2, snapshot.getTags().size());
        assertEquals("demo_db_14_backup", snapshot.getTag("Purpose"));
    }
}
