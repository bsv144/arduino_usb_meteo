/**
 * Created by Сергей on 17.07.2015.
 */

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.usb.*;

public class arduino_usb_meteo {

    /** The vendor ID of the usb  device. */
    private static final short VENDOR_ID = 0x2341;

    /** The product ID of the missile launcher. */
    private static final short PRODUCT_ID = 0x0043;


    public static void dump(UsbDevice device, int level)
    {
        for (int i = 0; i < level; i += 1) System.out.print("  ");
        System.out.println(device);
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
            {
                dump(child, level + 1);
            }
        }
    }



    /**
     * Dumps the specified USB device to stdout.
     *
     * @param device
     *            The USB device to dump.
     */
    private static void dumpDevice(final UsbDevice device)
    {
        // Dump information about the device itself
        System.out.println(device);
        final UsbPort port = device.getParentUsbPort();
        if (port != null)
        {
            System.out.println("Connected to port: " + port.getPortNumber());
            System.out.println("Parent: " + port.getUsbHub());
        }

        // Dump device descriptor
        System.out.println(device.getUsbDeviceDescriptor());

        // Process all configurations
        for (UsbConfiguration configuration: (List<UsbConfiguration>) device.getUsbConfigurations())
        {
            // Dump configuration descriptor
            System.out.println(configuration.getUsbConfigurationDescriptor());

            // Process all interfaces
            for (UsbInterface iface: (List<UsbInterface>) configuration
                    .getUsbInterfaces())
            {
                // Dump the interface descriptor
                System.out.println(iface.getUsbInterfaceDescriptor());

                // Process all endpoints
                for (UsbEndpoint endpoint: (List<UsbEndpoint>) iface
                        .getUsbEndpoints())
                {
                    // Dump the endpoint descriptor
                    System.out.println(endpoint.getUsbEndpointDescriptor());
                }
            }
        }

        System.out.println();

        // Dump child devices if device is a hub
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
            {
                dumpDevice(child);
            }
        }
    }

    /**
     * Dumps the name of the specified device to stdout.
     *
     * @param device
     *            The USB device.
     * @throws UnsupportedEncodingException
     *             When string descriptor could not be parsed.
     * @throws UsbException
     *             When string descriptor could not be read.
     */
    private static void dumpName(final UsbDevice device)
            throws UnsupportedEncodingException, UsbException
    {
        // Read the string descriptor indices from the device descriptor.
        // If they are missing then ignore the device.
        final UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
        final byte iManufacturer = desc.iManufacturer();
        final byte iProduct = desc.iProduct();
        if (iManufacturer == 0 || iProduct == 0) return;

        // Dump the device name
        System.out.println(device.getString(iManufacturer) + " "
                + device.getString(iProduct));
    }

    /**
     * Processes the specified USB device.
     *
     * @param device
     *            The USB device to process.
     */
    private static void processDevice(final UsbDevice device)
    {
        // When device is a hub then process all child devices
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
            {
                processDevice(child);
            }
        }

        // When device is not a hub then dump its name.
        else
        {
            try
            {
                dumpName(device);
            }
            catch (Exception e)
            {
                // On Linux this can fail because user has no write permission
                // on the USB device file. On Windows it can fail because
                // no libusb device driver is installed for the device
                System.err.println("Ignoring problematic device: " + e);
            }
        }
    }



    /**
     * Recursively searches for the specified device on the specified USB
     * hub and returns it. If there are multiple missile launchers attached then
     * this simple demo only returns the first one.
     *
     * @param hub
     *            The USB hub to search on.
     * @return The missile launcher USB device or null if not found.
     */
    public static UsbDevice findUSBDevice(UsbHub hub)
    {
        UsbDevice launcher = null;

        for (UsbDevice device: (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            if (device.isUsbHub())
            {
                launcher = findUSBDevice((UsbHub) device);
                if (launcher != null) return launcher;
            }
            else
            {
                UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
                if (desc.idVendor() == VENDOR_ID &&
                        desc.idProduct() == PRODUCT_ID) return device;
            }
        }
        return null;
    }

    public static void main(String[] args) throws UsbException
    {
        /** The vendor ID of the usb  device. */
        //final short VENDOR_ID = 0x2341;

        /** The product ID of the missile launcher. */
        //final short PRODUCT_ID = 0x0043;

        UsbDevice device = findUSBDevice(UsbHostManager.getUsbServices().getRootUsbHub());
        if (device == null)
        {
            System.err.println("Device not found.");
            System.exit(1);
            return;
        }
        UsbServices services = UsbHostManager.getUsbServices();
        System.out.println("USB Service Implementation: "
                + services.getImpDescription());
        System.out.println("Implementation version: "
                + services.getImpVersion());
        System.out.println("Service API version: " + services.getApiVersion());
        System.out.println("");
        //dump(services.getRootUsbHub(), 0);
        //dumpDevice(services.getRootUsbHub());
        //processDevice(services.getRootUsbHub());
        dumpDevice(device);
        processDevice(device);
    }
}
