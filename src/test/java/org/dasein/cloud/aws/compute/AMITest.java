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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.aws.AwsTestBase;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCopyOptions;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.MachineImageVolume;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
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
import java.util.Arrays;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private EC2Instance ec2Instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        ami = spy(new AMI(awsCloudStub));
        EC2ComputeServices computeServices = mock(EC2ComputeServices.class);
        doReturn(computeServices).when(awsCloudStub).getComputeServices();
        ec2Instance = mock(EC2Instance.class);
        doReturn(ec2Instance).when(computeServices).getVirtualMachineSupport();
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
        assertEquals(true, machineImage.isPublic());
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
                ami.listImages(ImageFilterOptions.getInstance().onPlatform(Platform.WINDOWS).matchingRegex(
                        "getting-started")));
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

    @Test
    public void testAddImageShare() throws Exception {
        String imageId = "ami-1a2b3c4d";
        String imageShareAccountNumber = "795219933132";

        doReturn(MachineImage.getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null))
                .when(ami).getImage(imageId);

        EC2Method modifyImageAttributeMock = mock(EC2Method.class);
        when(modifyImageAttributeMock.invoke()).thenReturn(resource("modify_image_attribute.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId),
                        hasEntry("LaunchPermission.Add.1.UserId", imageShareAccountNumber),
                        hasEntry("Action", "ModifyImageAttribute")))).thenReturn(modifyImageAttributeMock);

        doReturn(Arrays.asList(imageShareAccountNumber)).when(ami).listShares(imageId);

        ami.addImageShare(imageId, imageShareAccountNumber);

        verify(modifyImageAttributeMock, times(1)).invoke();
    }

    @Test
    public void testAddPublicShare() throws Exception {
        String imageId = "ami-1a2b3c4d";

        MachineImage privateMachineImage = MachineImage
                .getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null);
        MachineImage publicMachieImage = MachineImage
                .getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null)
                .sharedWithPublic();

        doReturn(privateMachineImage).doReturn(publicMachieImage).when(ami).getImage(imageId);

        EC2Method modifyImageAttributeMock = mock(EC2Method.class);
        when(modifyImageAttributeMock.invoke()).thenReturn(resource("modify_image_attribute.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId), hasEntry("LaunchPermission.Add.1.Group", "all"),
                        hasEntry("Action", "ModifyImageAttribute")))).thenReturn(modifyImageAttributeMock);

        ami.addPublicShare(imageId);

        verify(modifyImageAttributeMock, times(1)).invoke();
    }

    @Test
    public void testRemoveImageShare() throws Exception {
        String imageId = "ami-1a2b3c4d";
        String imageShareAccountNumber = "795219933132";

        doReturn(MachineImage.getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null))
                .when(ami).getImage(imageId);

        EC2Method modifyImageAttributeMock = mock(EC2Method.class);
        when(modifyImageAttributeMock.invoke()).thenReturn(resource("modify_image_attribute.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId),
                        hasEntry("LaunchPermission.Remove.1.UserId", imageShareAccountNumber),
                        hasEntry("Action", "ModifyImageAttribute")))).thenReturn(modifyImageAttributeMock);

        doReturn(Arrays.asList()).when(ami).listShares(imageId);

        ami.removeImageShare(imageId, imageShareAccountNumber);

        verify(modifyImageAttributeMock, times(1)).invoke();
    }

    @Test
    public void testRemovePublicShare() throws Exception {
        String imageId = "ami-1a2b3c4d";

        MachineImage privateMachineImage = MachineImage
                .getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null);
        MachineImage publicMachieImage = MachineImage
                .getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null)
                .sharedWithPublic();

        doReturn(publicMachieImage).doReturn(privateMachineImage).when(ami).getImage(imageId);

        EC2Method modifyImageAttributeMock = mock(EC2Method.class);
        when(modifyImageAttributeMock.invoke()).thenReturn(resource("modify_image_attribute.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId), hasEntry("LaunchPermission.Remove.1.Group", "all"),
                        hasEntry("Action", "ModifyImageAttribute")))).thenReturn(modifyImageAttributeMock);

        ami.removePublicShare(imageId);

        verify(modifyImageAttributeMock, times(1)).invoke();
    }

    @Test
    public void testRemoveAllImageShares() throws Exception {
        AMI powerAmi = PowerMockito.spy(new AMI(awsCloudStub));
        String imageId = "ami-1a2b3c4d";
        String imageShareAccountNumber = "795219933132";

        PowerMockito.doReturn(Arrays.asList(imageShareAccountNumber)).when(powerAmi, "sharesAsList", imageId);

        PowerMockito.doNothing().when(powerAmi, "setPrivateShare", imageId, false,
                new String[] { imageShareAccountNumber });
        PowerMockito.doNothing().when(powerAmi, "setPublicShare", imageId, false);

        powerAmi.removeAllImageShares(imageId);

        PowerMockito.verifyPrivate(powerAmi, times(1)).invoke("setPrivateShare", imageId, false,
                new String[] { imageShareAccountNumber });
        PowerMockito.verifyPrivate(powerAmi, times(1)).invoke("setPublicShare", imageId, false);
    }

    @Test
    public void testCopyImage() throws Exception {
        String imageId = "ami-1a2b3c4d";
        String targetRegion = "us-west-1";

        String name = "name";
        String description = "description";

        ProviderContext targetContext = mock(ProviderContext.class);
        doReturn(targetContext).when(providerContextStub).copy(targetRegion);
        AWSCloud targetProvider = PowerMockito.spy(new AWSCloud());
        doReturn(targetProvider).when(targetContext).connect();
        PowerMockito.doReturn(ACCOUNT_NO).when(targetProvider).testContext();

        EC2Method copyImageMock = mock(EC2Method.class);
        when(copyImageMock.invoke()).thenReturn(resource("copy_image.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(targetProvider),
                argThat(allOf(hasEntry("SourceRegion", REGION), hasEntry("SourceImageId", imageId),
                        hasEntry("Name", name), hasEntry("Description", description), hasEntry("Action", "CopyImage"))))
                .thenReturn(copyImageMock);

        String newImageId = ami.copyImage(ImageCopyOptions.getInstance(targetRegion, imageId, name, description));
        assertEquals("ami-4d3c2b1a", newImageId);
        verify(copyImageMock, times(1)).invoke();
    }

    @Test(expected = CloudException.class)
    public void testCopyImageTargetRegionTestConnectionFailed() throws Exception {
        String imageId = "ami-1a2b3c4d";
        String targetRegion = "us-west-1";

        ProviderContext targetContext = mock(ProviderContext.class);
        doReturn(targetContext).when(providerContextStub).copy(targetRegion);
        AWSCloud targetProvider = PowerMockito.spy(new AWSCloud());
        doReturn(targetProvider).when(targetContext).connect();
        PowerMockito.doReturn(null).when(targetProvider).testContext();

        ami.copyImage(ImageCopyOptions.getInstance(targetRegion, imageId, null, null));
    }

    @Test
    public void testRegisterImageBundle() throws Exception {
        String imageLocation = "myawsbucket/my-new-image.manifest.xml";
        String newImageId = "ami-1a2b3c4d";

        EC2Method registerImageMock = mock(EC2Method.class);
        when(registerImageMock.invoke()).thenReturn(resource("register_image.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageLocation", imageLocation), hasEntry("Action", "RegisterImage"))))
                .thenReturn(registerImageMock);

        MachineImage machineImage = MachineImage
                .getInstance(ACCOUNT_NO, REGION, newImageId, null, MachineImageState.ACTIVE, null, null, null, null);
        doReturn(machineImage).when(ami).getImage(newImageId);

        assertEquals(machineImage, ami.registerImageBundle(
                ImageCreateOptions.getInstance(MachineImageFormat.AWS, imageLocation, null, null, null)));

        verify(registerImageMock, times(1)).invoke();
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testRegisterImageBundleWithoutBundleLocation() throws Exception {
        ami.registerImageBundle(ImageCreateOptions.getInstance(MachineImageFormat.AWS, (String) null, null, null, null));
    }

    @Test(expected = CloudException.class)
    public void testRegisterImageBundleWithoutBundleFormat() throws Exception {
        String imageLocation = "myawsbucket/my-new-image.manifest.xml";
        ami.registerImageBundle(ImageCreateOptions.getInstance(null,imageLocation, null, null, null));
    }

    @Test(expected = CloudException.class)
    public void testRegisterImageBundleWithWrongBundleFormat() throws Exception {
        String imageLocation = "myawsbucket/my-new-image.manifest.xml";
        ami.registerImageBundle(ImageCreateOptions.getInstance(MachineImageFormat.VHD,imageLocation, null, null, null));
    }

    @Test
    public void testRemoveWithoutCheckState() throws Exception {
        String imageId = "ami-1a2b3c4d";

        EC2Method deregisterImageMock = mock(EC2Method.class);
        when(deregisterImageMock.invoke()).thenReturn(resource("deregister_image.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId), hasEntry("Action", "DeregisterImage"))))
                .thenReturn(deregisterImageMock);

        ami.remove(imageId, false);

        verify(deregisterImageMock, times(1)).invoke();
    }

    @Test
    public void testRemoveWithCheckState() throws Exception {
        String imageId = "ami-1a2b3c4d";

        doReturn(MachineImage.getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null))
                .when(ami).getImage(imageId);

        EC2Method deregisterImageMock = mock(EC2Method.class);
        when(deregisterImageMock.invoke()).thenReturn(resource("deregister_image.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("ImageId", imageId), hasEntry("Action", "DeregisterImage"))))
                .thenReturn(deregisterImageMock);

        ami.remove(imageId, true);

        verify(ami, times(1)).getImage(imageId);
        verify(deregisterImageMock, times(1)).invoke();
    }

    @Test
    public void testRemoveWithCheckStateIsDeleted() throws Exception {
        String imageId = "ami-1a2b3c4d";

        doReturn(MachineImage.getInstance(null, null, null, null, MachineImageState.DELETED, null, null, null, null))
                .when(ami).getImage(imageId);

        ami.remove(imageId, true);

        verify(ami, times(1)).getImage(imageId);
    }

    @Test(expected = CloudException.class)
    public void testCaptureImageWithInstanceNotExist() throws Exception {
        String sourceInstanceId = "i-1a2b3c4d";
        VirtualMachine sourceVirtualMachine = new VirtualMachine();
        sourceVirtualMachine.setProviderVirtualMachineId(sourceInstanceId);

        doReturn(null).when(ec2Instance).getVirtualMachine(sourceInstanceId);

        ami.captureImage(ImageCreateOptions.getInstance(sourceVirtualMachine, null, null));
    }

    @Test
    public void testCaptureImageWithLinux() throws Exception {
        String sourceInstanceId = "i-1a2b3c4d";
        VirtualMachine sourceVirtualMachine = new VirtualMachine();
        sourceVirtualMachine.setProviderVirtualMachineId(sourceInstanceId);
        sourceVirtualMachine.setPlatform(Platform.UBUNTU);
        sourceVirtualMachine.setCurrentState(VmState.STOPPED);

        doReturn(sourceVirtualMachine).when(ec2Instance).getVirtualMachine(sourceInstanceId);

        EC2Method createImageMock = mock(EC2Method.class);
        when(createImageMock.invoke()).thenReturn(resource("create_image.xml"));
        PowerMockito.whenNew(EC2Method.class).withArguments(eq(awsCloudStub),
                argThat(allOf(hasEntry("InstanceId", sourceInstanceId), hasEntry("Name", null),
                        hasEntry("Description", null), hasEntry("Action", "CreateImage")))).thenReturn(createImageMock);

        MachineImage newMachieImage = MachineImage
                .getInstance(null, null, null, null, MachineImageState.ACTIVE, null, null, null, null)
                .sharedWithPublic();

        doReturn(newMachieImage).when(ami).getImage("ami-4fa54026");

        MachineImage result = ami.captureImage(ImageCreateOptions.getInstance(sourceVirtualMachine, null, null));
        assertEquals(newMachieImage, result);
        verify(createImageMock, times(1)).invoke();
    }
}
