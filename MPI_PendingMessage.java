// When an MPI_Send or MPI_Recv is called, an MPI_PendingMessage instance is created.
// It will initially have either the src (source) or dst (destination) values filled.
// (src if this was created by an MPI_Send, and dst if it was by MPI_Recv).  In either
// case, the message will be stored on the destination.  When the matching MPI_Send or
// MPI_Recv is called, the message will actually be transferred.
//
// The message may be either blocking or non-blocking.  In addition, the message may
// contain its own buffer for the data, but this is all built into the data connections.
//
// This class will be subclassed to support integer, double, or string data.  The
// subclasses will have srcData and dstData arrays.

public class MPI_PendingMessage
{
    // These form a linked-list in the destination, with the latest message being at
    // the start of the list.
    private MPI_PendingMessage _next;

    // References to the source and destination process:
    private MPI_Proc _src, _dst;

    // Is the source MPI_Proc blocked?  Is the destination?
    private boolean _srcBlocked, _dstBlocked;

    // The message may have a tag:
    private int _tag;

    // The MPI_DataConnections for the source and destination of the transfer
    private MPI_DataConnection _dataSrc, _dataDst;

    public MPI_PendingMessage(MPI_Proc src, MPI_Proc dst, int tag, MPI_PendingMessage next)
    {
        _next = next;
        _src = src;
        _dst = dst;
        _srcBlocked = _dstBlocked = false;
        _tag = tag;
        _dataSrc = null;
        _dataDst = null;
    }

    public void setSource(MPI_DataConnection src)
    {
        _dataSrc = src;
    }

    public void setDestination(MPI_DataConnection dst)
    {
        _dataDst = dst;
    }

    public MPI_Proc from() { return _src; }
    public MPI_Proc to() { return _dst; }
    public int tag() { return _tag; }
    public MPI_DataConnection.MPI_Datatype datatype()
    {
        if (_dataSrc != null)
        {
            return _dataSrc.datatype();
        }
        return _dataDst.datatype();
    }

    public MPI_PendingMessage next() { return _next; }

    public void removeNext() { if (_next != null) { _next = _next._next; } }

    public boolean hasSource() { return _dataSrc != null; }
    public MPI_DataConnection dataSrc() { return _dataSrc; }

    public boolean hasDestination() { return _dataDst != null; }

    public void setSourceBlocked() { _srcBlocked = true; }
    public void clearSourceBlocked() { _srcBlocked = false; }
    public boolean sourceBlocked() { return _srcBlocked; }

    public void setDestinationBlocked() { _dstBlocked = true; }
    public void clearDestinationBlocked() { _dstBlocked = false; }
    public boolean destinationBlocked() { return _dstBlocked; }

    // This method is called to actually perform the transfer of the data.  This
    // is subclassed to move the actual data, but common portions of the code
    // are handled here.
    public void sendMessage()
    {
        _dataDst.transferFrom(_dataSrc, 0, 0);
        _dst.removeMessage(this);
    }

    public void status(boolean sendToErr, MPI_World world)
    {
        String msg = "...[From: " + _src.rank();
        if (_srcBlocked)
        {
            msg += "(blocked)";
        }
        msg += ", To: " + _dst.rank();
        if (_dstBlocked)
        {
            msg += "(blocked)";
        }
        msg += ", Tag: " + _tag + "]";
        if (_dataSrc != null)
        {
            msg += " has source conn";
        }
        if (_dataDst != null)
        {
            msg += " has destination conn";
        }
        if (sendToErr)
        {
            world.error(-1, msg);
        }
        else
        {
            System.out.println(msg);
        }
    }
}
