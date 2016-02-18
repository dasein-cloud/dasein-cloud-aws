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

import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.MachineImageVolume;
import org.dasein.cloud.compute.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Jeffrey Yan on 2/17/2016.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = { AWSCloud.class, AMI.class }, fullyQualifiedNames = "org.dasein.cloud.aws.compute.AMI*")
public class AMITest extends AwsTestBase {

    private AMI ami;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        ami = new AMI(awsCloudStub);
    }

    protected Document resource(String resourceName) throws Exception {
        return super.resource("org/dasein/cloud/aws/compute/ami/" + resourceName);
    }

    protected InputStream stream(String resourceName) throws Exception {
        return getClass().getClassLoader().getResourceAsStream("org/dasein/cloud/aws/compute/ami/" + resourceName);
    }

    @Test
    public void testGetImage() throws Exception {
        String imageId = "ami-1a2b3c4d";

        EC2Method describeImagesStub = mock(EC2Method.class);
        when(describeImagesStub.invoke()).thenReturn(resource("describe_images_single.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub);

        MachineImage machineImage = ami.getImage(imageId);
        assertEquals(ACCOUNT_NO, machineImage.getProviderOwnerId());
        assertEquals(MachineImageState.ACTIVE, machineImage.getCurrentState());
        assertEquals(ACCOUNT_NO, machineImage.getProviderOwnerId());
        assertEquals("true", machineImage.getTag("public"));
        assertEquals(Architecture.I32, machineImage.getArchitecture());
        assertEquals(ImageClass.MACHINE, machineImage.getImageClass());
        assertEquals(Platform.WINDOWS, machineImage.getPlatform());
        assertEquals("getting-started", machineImage.getName());
        assertEquals("Image Description", machineImage.getDescription());
        assertEquals("us-east-1", machineImage.getProviderRegionId());
        assertEquals(MachineImageType.VOLUME, machineImage.getType());

        List<MachineImageVolume> volumes = toList(machineImage.getVolumes());
        assertEquals(1, volumes.size());
        assertEquals("/dev/sda1", volumes.get(0).getDeviceName());
        assertEquals("snap-1a2b3c4d", volumes.get(0).getSnapshotId());
        assertEquals(Integer.valueOf(15), volumes.get(0).getVolumeSize());
        assertEquals("standard", volumes.get(0).getVolumeType());
    }

    @Test
    public void testIsImageSharedWithPublic() throws Exception {
        String imageId = "ami-1a2b3c4d";

        EC2Method describeImagesStub = mock(EC2Method.class);
        when(describeImagesStub.invoke()).thenReturn(resource("describe_images_single.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub);

        assertTrue(ami.isImageSharedWithPublic(imageId));
    }

    @Test
    public void testIsSubscribedTrue() throws Exception {
        EC2Method describeImagesStub = mock(EC2Method.class);
        when(describeImagesStub.invoke()).thenReturn(resource("describe_images_single.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Owner", ACCOUNT_NO), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub);

        assertTrue(ami.isSubscribed());
    }

    @Test
    public void testIsSubscribedFalse() throws Exception {
        EC2Method describeImagesStub = mock(EC2Method.class);
        when(describeImagesStub.invoke()).thenThrow(EC2Exception
                .create(401, "request id", "error code", "not able to validate the provided access credentials"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Owner", ACCOUNT_NO), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub);

        assertFalse(ami.isSubscribed());
    }

    @Test
    public void testListImageStatus() throws Exception {
        EC2Method describeImagesStub1 = mock(EC2Method.class);
        when(describeImagesStub1.invoke()).thenReturn(resource("describe_images_list.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ExecutableBy.1", "self"), hasEntry("Filter.1.Name", "image-type"),
                        hasEntry("Filter.1.Value", "machine"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub1);

        EC2Method describeImagesStub2 = mock(EC2Method.class);
        when(describeImagesStub2.invoke()).thenReturn(resource("describe_images_single.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Owner", "self"), hasEntry("Filter.1.Name", "image-type"),
                        hasEntry("Filter.1.Value", "machine"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub2);

        EC2Method describeImagesStub3 = mock(EC2Method.class);
        when(describeImagesStub3.invoke()).thenReturn(resource("describe_images_single.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ExecutableBy", ACCOUNT_NO), hasEntry("Filter.1.Name", "image-type"),
                        hasEntry("Filter.1.Value", "machine"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub3);

        List<ResourceStatus> resourceStatuses = toList(ami.listImageStatus(ImageClass.MACHINE));
        assertEquals(3, resourceStatuses.size());
        assertEquals(MachineImageState.ACTIVE, resourceStatuses.get(0).getResourceStatus());
        assertEquals(MachineImageState.PENDING, resourceStatuses.get(1).getResourceStatus());
        assertEquals(MachineImageState.ACTIVE, resourceStatuses.get(2).getResourceStatus());
    }

    private Stubber parse(final String resource) {
        return doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                XmlStreamParser parser = invocation.getArgumentAt(0, XmlStreamParser.class);
                parser.parse(stream(resource));
                return null;
            }
        });
    }

    @Test
    public void testListImagesWithoutFilter() throws Exception {
        EC2Method describeImagesStub1 = mock(EC2Method.class);
        parse("describe_images_list.xml").when(describeImagesStub1).invoke((XmlStreamParser) notNull());
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ExecutableBy.1", "self"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub1);

        EC2Method describeImagesStub2 = mock(EC2Method.class);
        parse("describe_images_single.xml").when(describeImagesStub2).invoke((XmlStreamParser) notNull());
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Owner", "self"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub2);

        List<MachineImage> machineImages = toList(ami.listImages(ImageFilterOptions.getInstance()));
        assertEquals(3, machineImages.size());
    }

    @Test
    public void testListImagesWithPlatformFilter() throws Exception {
        EC2Method describeImagesStub1 = mock(EC2Method.class);
        parse("describe_images_list.xml").when(describeImagesStub1).invoke((XmlStreamParser) notNull());
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ExecutableBy.1", "self"), hasEntry("Filter.1.Name", "platform"),
                        hasEntry("Filter.1.Value.1", "windows"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub1);

        EC2Method describeImagesStub2 = mock(EC2Method.class);
        parse("describe_images_single.xml").when(describeImagesStub2).invoke((XmlStreamParser) notNull());
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("Owner", "self"), hasEntry("Filter.1.Name", "platform"),
                        hasEntry("Filter.1.Value.1", "windows"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub2);

        List<MachineImage> machineImages = toList(
                ami.listImages(ImageFilterOptions.getInstance().onPlatform(Platform.WINDOWS).matchingRegex("getting-started")));
        assertEquals(2, machineImages.size());
    }

    @Test
    public void testSearchPublicImages() throws Exception {
        EC2Method describeImagesStub1 = mock(EC2Method.class);
        parse("describe_images_list.xml").when(describeImagesStub1).invoke((XmlStreamParser) notNull());
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ExecutableBy.1", "self"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub1);

        EC2Method describeImagesStub2 = mock(EC2Method.class);
        parse("describe_images_single.xml").when(describeImagesStub2).invoke((XmlStreamParser) notNull());
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ExecutableBy.1", "all"), hasEntry("Action", "DescribeImages"))))
                .thenReturn(describeImagesStub2);

        List<MachineImage> machineImages = toList(ami.searchPublicImages(ImageFilterOptions.getInstance()));
        assertEquals(3, machineImages.size());
    }

    @Test
    public void testListShares() throws Exception {
        String imageId = "ami-1a2b3c4d";

        EC2Method describeImageAttributeStub = mock(EC2Method.class);
        when(describeImageAttributeStub.invoke()).thenReturn(resource("describe_image_attribute_launch_permission.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId), hasEntry("Attribute", "launchPermission"),
                        hasEntry("Action", "DescribeImageAttribute")))).thenReturn(describeImageAttributeStub);

        List<String> shares = toList(ami.listShares(imageId));
        assertEquals(2, shares.size());
        assertTrue(shares.contains("495219933132"));
        assertTrue(shares.contains("595219933132"));
    }
}
