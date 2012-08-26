package com.codingstory.polaris.sandbox;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SampleThriftTest {

    @Test
    public void testEncodeDecode() throws TException {
        SampleRecord record = new SampleRecord();
        record.setField1(10);
        record.setField2("hello");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encode(out, record);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        record = decode(in);

        Assert.assertEquals(10, record.getField1());
        Assert.assertEquals("hello", record.getField2());
    }

    private SampleRecord decode(ByteArrayInputStream in) throws TException {
        TTransport transport = new TIOStreamTransport(in);
        TProtocol protocol = new TBinaryProtocol(transport);
        SampleRecord record = new SampleRecord();
        record.read(protocol);
        return record;
    }

    private static void encode(ByteArrayOutputStream out, SampleRecord record) throws TException {
        TTransport transport = new TIOStreamTransport(out);
        TProtocol protocol = new TBinaryProtocol(transport);
        record.write(protocol);
    }


}
