import jdk.internal.org.objectweb.asm.Handle;

import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by Родители on 19.07.2015.
 */
public class usbArduino {
    public UsbDevice findDevice(UsbHub hub, short vendorId, short productId)
    {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            if (device.isUsbHub())
            {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null) return device;
            }
        }
        return null;
    }

    public static void main(String[] args) throws UsbException, UnsupportedEncodingException {
        /** The vendor ID of the usb  device. */
        final short VENDOR_ID = 0x2341;

        /** The product ID of the missile launcher. */
        final short PRODUCT_ID = 0x0043;

        usbArduino arduino = new usbArduino();
        UsbDevice dev = arduino.findDevice(UsbHostManager.getUsbServices().getRootUsbHub(), VENDOR_ID, PRODUCT_ID);
        System.out.println(dev.getUsbDeviceDescriptor());
        //System.out.println(iface.getUsbInterfaceDescriptor());
        //System.out.println(endpoint.getUsbEndpointDescriptor());

        /** This example reads the current configuration number from a device by using a control request: */
        UsbControlIrp irp = dev.createUsbControlIrp(
                (byte) (UsbConst.REQUESTTYPE_DIRECTION_IN
                        | UsbConst.REQUESTTYPE_TYPE_STANDARD
                        | UsbConst.REQUESTTYPE_RECIPIENT_DEVICE),
                UsbConst.REQUEST_GET_CONFIGURATION,
                (short) 0,
                (short) 0
        );
        irp.setData(new byte[1]);

        dev.syncSubmit(irp);
        System.out.println("Configuration number: " + irp.getData()[0]);


        UsbConfiguration configuration = dev.getActiveUsbConfiguration();
        UsbInterface iface = configuration.getUsbInterface((byte) 1);
        System.out.println("Interface descriptor:");
        System.out.println(iface.getUsbInterfaceDescriptor());
        if(!iface.isClaimed()) {
            System.out.println("need claim");
            iface.claim(new UsbInterfacePolicy() {
                @Override
                public boolean forceClaim(UsbInterface usbInterface) {
                    return true;
                }
            });
        };
        try {
            UsbEndpoint endpoint = iface.getUsbEndpoint((byte) 0x83);
            System.out.println(endpoint.getDirection());
            System.out.println(endpoint.getUsbEndpointDescriptor());

            final UsbPipe pipe = endpoint.getUsbPipe();

            pipe.addUsbPipeListener(new UsbPipeListener() {
                @Override
                public void errorEventOccurred(UsbPipeErrorEvent event) {
                    UsbException error = event.getUsbException();
                    //... Handle error ...
                    System.out.println(error);
                }

                @Override
                public void dataEventOccurred(UsbPipeDataEvent event) {
                    byte[] data = event.getData();
                    UsbIrp received = null;
                    try {
                        received = pipe.asyncSubmit(data);
                    } catch (UsbException e) {
                        e.printStackTrace();
                    }
                    System.out.println(received.getLength() + " bytes received");
                    System.out.println(received.getData());
                    //... Process received data ...
                }
            });
            pipe.open();
            /**
             try {
                byte[] data = new byte[64];
                int received = pipe.asyncSubmit(data);
                System.out.println(received + " bytes received");
                System.out.println(data);
            } finally {
                pipe.close();
            }
             */
           while(true){

           }
        } finally {
            iface.release();
        }

    }

}
