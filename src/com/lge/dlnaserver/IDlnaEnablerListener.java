/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\1_android\\project\\DlnaServer\\src\\com\\lge\\dlnaserver\\IDlnaEnablerListener.aidl
 */
package com.lge.dlnaserver;
public interface IDlnaEnablerListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.lge.dlnaserver.IDlnaEnablerListener
{
private static final java.lang.String DESCRIPTOR = "com.lge.dlnaserver.IDlnaEnablerListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.lge.dlnaserver.IDlnaEnablerListener interface,
 * generating a proxy if needed.
 */
public static com.lge.dlnaserver.IDlnaEnablerListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.lge.dlnaserver.IDlnaEnablerListener))) {
return ((com.lge.dlnaserver.IDlnaEnablerListener)iin);
}
return new com.lge.dlnaserver.IDlnaEnablerListener.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_startShareComplete:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.startShareComplete(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_stopShareComplete:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.stopShareComplete(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_statusUpdate:
{
data.enforceInterface(DESCRIPTOR);
this.statusUpdate();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.lge.dlnaserver.IDlnaEnablerListener
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void startShareComplete(int err) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(err);
mRemote.transact(Stub.TRANSACTION_startShareComplete, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void stopShareComplete(int err) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(err);
mRemote.transact(Stub.TRANSACTION_stopShareComplete, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void statusUpdate() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_statusUpdate, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_startShareComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_stopShareComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_statusUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public void startShareComplete(int err) throws android.os.RemoteException;
public void stopShareComplete(int err) throws android.os.RemoteException;
public void statusUpdate() throws android.os.RemoteException;
}
