// This class is one instance of a process (thread).  This will be subclassed,
// with the subclass implementing the 'run' routine.
import java.awt.Color;

public class MPI_Proc implements Runnable
{
    public enum MPI_ProcState { INITIALIZED, STARTED, RUNNING, BLOCKED, FINALIZED, STOPPED };
    final public MPI_DataConnection.MPI_Datatype MPI_INT = MPI_DataConnection.MPI_Datatype.MPI_DT_INT;
    final public MPI_DataConnection.MPI_Datatype MPI_DOUBLE = MPI_DataConnection.MPI_Datatype.MPI_DT_DOUBLE;
    final public MPI_DataConnection.MPI_Datatype MPI_STRING = MPI_DataConnection.MPI_Datatype.MPI_DT_STRING;
    final public int MPI_COMM_WORLD = 82736154;
    final public int MPI_ANY_SOURCE = -1;
    final public int MPI_ANY_TAG = -1;
    final public MPI_Status MPI_STATUS_IGNORE = null;
    public enum MPI_ReduceOp { MAX, MIN, SUM, PROD, LAND, LOR, BAND, BOR, MAXLOC, MINLOC };
    final public MPI_ReduceOp MPI_MAX = MPI_ReduceOp.MAX;
    final public MPI_ReduceOp MPI_MIN = MPI_ReduceOp.MIN;
    final public MPI_ReduceOp MPI_SUM = MPI_ReduceOp.SUM;
    final public MPI_ReduceOp MPI_PROD = MPI_ReduceOp.PROD;
    final public MPI_ReduceOp MPI_LAND = MPI_ReduceOp.LAND;
    final public MPI_ReduceOp MPI_LOR = MPI_ReduceOp.LOR;
    final public MPI_ReduceOp MPI_BAND = MPI_ReduceOp.BAND;
    final public MPI_ReduceOp MPI_BOR = MPI_ReduceOp.BOR;
    final public MPI_ReduceOp MPI_MAXLOC = MPI_ReduceOp.MAXLOC;
    final public MPI_ReduceOp MPI_MINLOC = MPI_ReduceOp.MINLOC;

    private Thread _myThread;

    // Pointer to the 'World', the collection of all the processes
    private MPI_World _world;

    // The rank (index) for this process
    private int _rank;

    private MPI_ProcState _state;

    // If this process is blocked waiting for Send/Recv with another process,
    // this will hold that process:
    private MPI_Proc _imBlockedForProc;

    // If this process is blocked waiting for a collective, this is the collective
    private MPI_Collective _imBlockedForCollective;

    // The list of pending messages for this process:
    private MPI_PendingMessage _messages;

    // To handle the collective messaging, each proc will have the data connection(s),
    // stored here.  The collective's code will do the data transfers.
    private MPI_DataConnection _collectiveConn;
    private MPI_DataConnection _collectiveConn2;

    private QV_View _myView;

    public MPI_Proc(MPI_World world, int rank)
    {
        _world = world;
        _rank = rank;
        _messages = null;
        _state = MPI_ProcState.INITIALIZED;
        _imBlockedForProc = null;
        _imBlockedForCollective = null;
        _collectiveConn = null;
        _collectiveConn2 = null;
        _myView = null;
    }

    public void setThread(Thread thread)
    {
        _myThread = thread;
    }

    public void start()
    {
        _myThread.start();
    }

    public int rank() { return _rank; }
    public MPI_DataConnection collectiveConn() { return _collectiveConn; }
    public MPI_DataConnection collectiveConn2() { return _collectiveConn2; }

    public void run()
    {
        _state = MPI_ProcState.STARTED;

        try
        {
            exec(0, null);
        }
        catch (InterruptedException e)
        {
            ;
        }

        if (_state != MPI_ProcState.FINALIZED)
        {
            _world.error("Process " + _rank + " in state " + ProcStateToString(_state) + " but expected " + ProcStateToString(MPI_ProcState.FINALIZED));
        }
        _state = MPI_ProcState.STOPPED;
        _myView = null;
    }

    // This function is overwritten
    public void exec(int argc, String argv[]) throws InterruptedException
    {
        MPI_Init(argc, argv);
        Thread.sleep(1000);

        MPI_Finalize();
    }

    public String ProcStateToString(MPI_ProcState state)
    {
        switch (state)
        {
        case STARTED:       return("STARTED");
        case INITIALIZED:   return("INITIALIZED");
        case RUNNING:       return("RUNNING");
        case BLOCKED:       return("BLOCKED");
        case FINALIZED:     return("FINALIZED");
        case STOPPED:       return("STOPPED");
        }
        return("UNKNOWN");
    }

    public void MPI_Init(int argc, String argv[])
    {
        if (_state != MPI_ProcState.STARTED)
        {
            _world.error(1, "Process " + _rank + " in state " + ProcStateToString(_state) + " but expected " + ProcStateToString(MPI_ProcState.STARTED));
        }
        _state = MPI_ProcState.BLOCKED;
        startCollective(MPI_Collective.MPI_CollectiveType.INIT, MPI_ReduceOp.SUM, null);
        _state = MPI_ProcState.RUNNING;
    }

    public void MPI_Finalize()
    {
        if (_state != MPI_ProcState.RUNNING)
        {
            _world.error(1, "Process " + _rank + " in state " + ProcStateToString(_state) + " but expected " + ProcStateToString(MPI_ProcState.RUNNING));
        }
        _state = MPI_ProcState.BLOCKED;
        startCollective(MPI_Collective.MPI_CollectiveType.FINALIZE, MPI_ReduceOp.SUM, null);
        _state = MPI_ProcState.FINALIZED;
    }

    public void MPI_Barrier()
    {
        _state = MPI_ProcState.BLOCKED;
        startCollective(MPI_Collective.MPI_CollectiveType.BARRIER, MPI_ReduceOp.SUM, null);
        _state = MPI_ProcState.RUNNING;
    }

    public int MPI_Comm_size(int comm)
    {
        if (_state != MPI_ProcState.RUNNING)
        {
            _world.error(2, "Process " + _rank + " in state " + ProcStateToString(_state) + " but expected " + ProcStateToString(MPI_ProcState.RUNNING));
        }
        if (comm != MPI_COMM_WORLD)
        {
            _world.error(2, "Process " + _rank + " Communicator not set to MPI_COMM_WORLD");
        }
        return(_world.numProcs());
    }

    public int MPI_Comm_rank(int comm)
    {
        if (_state != MPI_ProcState.RUNNING)
        {
            _world.error(2, "Process " + _rank + " in state " + ProcStateToString(_state) + " but expected " + ProcStateToString(MPI_ProcState.RUNNING));
        }
        if (comm != MPI_COMM_WORLD)
        {
            _world.error(2, "Process " + _rank + " Communicator not set to MPI_COMM_WORLD");
        }
        return(_rank);
    }

    public MPI_ProcState state() { return _state; }
    public MPI_Proc imBlockedForProc() { return _imBlockedForProc; }
    public MPI_Collective imBlockedForCollective() { return _imBlockedForCollective; }
    public void setImBlockedForCollective(MPI_Collective collective) { _imBlockedForCollective = collective; }

    ////////////////////////////////////////////////////////////////////
    // Check for a deadlock situation.  We are considering having this
    // process block waiting for another process.  Report an error if
    // that would make a loop.
    private void checkForDeadlock(MPI_Proc other, int depth, boolean doSend)
    {
        if (other._imBlockedForCollective != null)
        {
            String msg = "Process " + _rank + " cannot do a ";
            msg += doSend ? "MPI_Send" : "MPI_Recv";
            msg += " with process " + other._rank + " because that process is in a "
                    + MPI_Collective.typeAsString(other._imBlockedForCollective.type());
            _world.error(depth, msg);
            return;
        }
        int deadman = _world.numProcs();
        MPI_Proc x = other;
        boolean err = false;
        for (; deadman > 0; deadman--)
        {
            if (x._imBlockedForProc == this)
            {
                err = true;
                break;
            }
            x = x._imBlockedForProc;
            if (x == null)
            {
                return;
            }
        }
        if (err)
        {
            _world.error(depth, "Deadlock detected involving these processes:");
            String msg = "Attempting to add an " + (doSend ? "MPI_Send" : "MPI_Recv")
                    + " between processes " + _rank + " and " + other._rank;
            _world.error(-1, msg);
            status(true);
            deadman = _world.numProcs();
            x = other;
            for (; deadman > 0; deadman--)
            {
                x.status(true);
                if (x._imBlockedForProc == this)
                {
                    break;
                }
                x = x._imBlockedForProc;
                if (x == null)
                {
                    return;
                }
            }
            _world.error(depth, msg);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // The common routine for doing an MPI_Send
    private void doSend(MPI_DataConnection conn, MPI_DataConnection.MPI_Datatype datatype,
                        int destID, int tag, int comm, boolean blocking)
    {
        if (_state != MPI_ProcState.RUNNING)
        {
            _world.error(4, "Process " + _rank + " in state " + ProcStateToString(_state) + " but expected " + ProcStateToString(MPI_ProcState.RUNNING));
        }

        // Verify datatype and comm
        if (datatype != conn.datatype())
        {
            _world.error(4, "Sending " + MPI_DataConnection.datatypeString(conn.datatype())
                    + " data, but request says " + MPI_DataConnection.datatypeString(datatype));
            return;
        }
        if (comm != MPI_COMM_WORLD)
        {
            _world.error(4, "Process " + _rank + " Communicator not set to MPI_COMM_WORLD");
        }

        MPI_Proc dest = _world.findProc(destID);
        if (dest == null)
        {
            _world.error(4, "No process with rank " + destID);
            return;
        }

        synchronized(_world)
        {
            // See if the destination has a pending message (is waiting for me)
            MPI_PendingMessage msg = dest.getDstMessage(_rank, tag, datatype);
            while (msg != null && msg.hasSource())
            {
                // There is already a pending message from us, so we block until that
                // message is taken.
                checkForDeadlock(dest, 5, true);
                msg.setSourceBlocked();
                _imBlockedForProc = dest;
                while (msg.sourceBlocked())
                {
                    try
                    {
                        _world.wait();
                    }
                    catch (InterruptedException e)
                    {
                        // Empty
                    }
                }
                _imBlockedForProc = null;
                _world.notifyAll();
                // That message should be taken.  See if there is ANOTHER one
                // (there shouldn't be...)
                msg = dest.getDstMessage(_rank, tag, datatype);
            }
            if (msg != null)
            {
                // We have a message, but it must have come from the destination
                // (because it doesn't have a source).  Send to it.
                // Set the src values for this message
                msg.setSource(conn);

                msg.clearDestinationBlocked();

                // Send the actual message, then return
                msg.sendMessage();
                dest._imBlockedForProc = null;
                _world.notifyAll();
                return;
            }

            // There wasn't a pending message, so our only option is to make one, then
            // block for it.  First check to see if this would cause deadlock!
            if (blocking)
            {
                checkForDeadlock(dest, 5, true);
            }

            // There wasn't a pending message, so create one here, then wait (if we block)
            msg = dest.createNewMessage(this, tag);
            msg.setSource(conn);
            if (blocking)
            {
                msg.setSourceBlocked();
                _imBlockedForProc = dest;
                while (msg.sourceBlocked())
                {
                    try
                    {
                        _world.wait();
                    }
                    catch (InterruptedException e)
                    {
                        // Empty
                    }
                }
                _imBlockedForProc = null;
                _world.notifyAll();
            }
        }
    }

    // The general form of the MPI_Send function for integer data (may or may not block)
    protected void MPI_Send(int []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionInt(_world, data, count, false);
        doSend(conn, datatype, destID, tag, comm, true);
    }

    // The blocking (synchronous) version of the MPI_Send function for integer data
    protected void MPI_Ssend(int []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionInt(_world, data, count, false);
        doSend(conn, datatype, destID, tag, comm, true);
    }

    // The non-blocking (asynchronous) version of the MPI_Send function for integer data
    protected void MPI_Isend(int []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionInt(_world, data, count, true);
        doSend(conn, datatype, destID, tag, comm, false);
    }

    // The general form of the MPI_Send function for double data (may or may not block)
    protected void MPI_Send(double []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionDouble(_world, data, count, false);
        doSend(conn, datatype, destID, tag, comm, true);
    }

    // The blocking (synchronous) version of the MPI_Send function for double data
    protected void MPI_Ssend(double []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionDouble(_world, data, count, false);
        doSend(conn, datatype, destID, tag, comm, true);
    }

    // The non-blocking (asynchronous) version of the MPI_Send function for double data
    protected void MPI_Isend(double []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionDouble(_world, data, count, true);
        doSend(conn, datatype, destID, tag, comm, false);
    }

    // The general form of the MPI_Send function for string data (may or may not block)
    protected void MPI_Send(String []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionString(_world, data, count, false);
        doSend(conn, datatype, destID, tag, comm, true);
    }

    // The blocking (synchronous) version of the MPI_Send function for string data
    protected void MPI_Ssend(String []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionString(_world, data, count, false);
        doSend(conn, datatype, destID, tag, comm, true);
    }

    // The non-blocking (asynchronous) version of the MPI_Send function for string data
    protected void MPI_Isend(String []data, int count, MPI_DataConnection.MPI_Datatype datatype, int destID, int tag, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionString(_world, data, count, true);
        doSend(conn, datatype, destID, tag, comm, false);
    }

    ////////////////////////////////////////////////////////////////////
    // The general MPI_Recv function
    private void doRecv(MPI_DataConnection conn, MPI_DataConnection.MPI_Datatype datatype, int srcID, int tag, int comm, MPI_Status status)
    {
        if (_state != MPI_ProcState.RUNNING)
        {
            _world.error(4, "Process " + _rank + " in state " + ProcStateToString(_state) + " but expected " + ProcStateToString(MPI_ProcState.RUNNING));
        }

        // Verify datatype and comm
        if (datatype != conn.datatype())
        {
            _world.error(4, "Receiving " + MPI_DataConnection.datatypeString(conn.datatype())
                    + " data, but request says " + MPI_DataConnection.datatypeString(datatype));
            return;
        }
        if (comm != MPI_COMM_WORLD)
        {
            _world.error(4, "Process " + _rank + " Communicator not set to MPI_COMM_WORLD");
        }

        MPI_Proc src = _world.findProc(srcID);
        if (src == null)
        {
            _world.error(4, "No process with rank " + srcID);
            return;
        }

        // See if there is a pending message (is waiting for me)
        synchronized(_world)
        {
            MPI_PendingMessage msg = getDstMessage(srcID, tag, datatype);
            if (msg != null)
            {
                // We have a message, receive from it.
                // Set the dst values for this message
                msg.setDestination(conn);
                msg.clearSourceBlocked();

                // Send the actual message, then return
                msg.sendMessage();
                if (status != MPI_STATUS_IGNORE)
                {
                    status.MPI_SOURCE = msg.from().rank();
                    status.MPI_TAG = msg.tag();
                    status.MPI_COUNT = msg.dataSrc().count();
                }
                src._imBlockedForProc = null;
                _world.notifyAll();
                return;
            }

            // There wasn't a pending message, so create one here, then wait
            msg = createNewMessage(src, tag);
            msg.setDestination(conn);
            checkForDeadlock(src, 5, false);
            msg.setDestinationBlocked();
            _imBlockedForProc = src;
            while (msg.destinationBlocked())
            {
                try
                {
                    _world.wait();
                }
                catch (InterruptedException e)
                {
                    // Empty
                }
            }
            if (status != MPI_STATUS_IGNORE)
            {
                status.MPI_SOURCE = msg.from().rank();
                status.MPI_TAG = msg.tag();
                status.MPI_COUNT = msg.dataSrc().count();
            }
            _imBlockedForProc = null;
            _world.notifyAll();
        }
    }

    // The blocking (synchronous) version of the MPI_Recv function for integer data
    protected void MPI_Recv(int []data, int count, MPI_DataConnection.MPI_Datatype datatype, int srcID, int tag, int comm, MPI_Status status)
    {
        MPI_DataConnection conn = new MPI_DataConnectionInt(_world, data, count, false);
        doRecv(conn, datatype, srcID, tag, comm, status);
    }

    // The blocking (synchronous) version of the MPI_Recv function for double data
    protected void MPI_Recv(double []data, int count, MPI_DataConnection.MPI_Datatype datatype, int srcID, int tag, int comm, MPI_Status status)
    {
        MPI_DataConnection conn = new MPI_DataConnectionDouble(_world, data, count, false);
        doRecv(conn, datatype, srcID, tag, comm, status);
    }

    // The blocking (synchronous) version of the MPI_Recv function for string data
    protected void MPI_Recv(String []data, int count, MPI_DataConnection.MPI_Datatype datatype, int srcID, int tag, int comm, MPI_Status status)
    {
        MPI_DataConnection conn = new MPI_DataConnectionString(_world, data, count, false);
        doRecv(conn, datatype, srcID, tag, comm, status);
    }

    ////////////////////////////////////////////////////////////////////
    // The MPI_Probe function
    private void MPI_Probe(int srcID, int tag, int comm, MPI_Status status)
    {
        // TBD
    }

    ////////////////////////////////////////////////////////////////////
    // The general MPI_Bcast function
    private void doBcast(MPI_DataConnection conn, MPI_DataConnection.MPI_Datatype datatype, int rootID, int comm)
    {
        _state = MPI_ProcState.BLOCKED;
        _collectiveConn = conn;
        startCollective(MPI_Collective.MPI_CollectiveType.BCAST, MPI_ReduceOp.SUM, _world.findProc(rootID));
        _collectiveConn = null;
        _state = MPI_ProcState.RUNNING;
    }

    protected void MPI_Bcast(int []data, int count, MPI_DataConnection.MPI_Datatype datatype, int rootID, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionInt(_world, data, count, false);
        doBcast(conn, datatype, rootID, comm);
    }

    protected void MPI_Bcast(double []data, int count, MPI_DataConnection.MPI_Datatype datatype, int rootID, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionDouble(_world, data, count, false);
        doBcast(conn, datatype, rootID, comm);
    }

    protected void MPI_Bcast(String []data, int count, MPI_DataConnection.MPI_Datatype datatype, int rootID, int comm)
    {
        MPI_DataConnection conn = new MPI_DataConnectionString(_world, data, count, false);
        doBcast(conn, datatype, rootID, comm);
    }

    ////////////////////////////////////////////////////////////////////
    // The general MPI_Scatter function
    private void doScatter(MPI_DataConnection sConn, MPI_DataConnection dConn, int rootID, int comm)
    {
        int actualLength = sConn.actualLength();
        int neededLength = _world.numProcs() * sConn.count();
        if (actualLength < neededLength)
        {
            _world.error(2, "The Scatter source array should have size " + neededLength + " but has size " + actualLength);
        }
        _state = MPI_ProcState.BLOCKED;
        _collectiveConn = sConn;
        _collectiveConn2 = dConn;
        startCollective(MPI_Collective.MPI_CollectiveType.SCATTER, MPI_ReduceOp.SUM, _world.findProc(rootID));
        _collectiveConn = null;
        _collectiveConn2 = null;
        _state = MPI_ProcState.RUNNING;
    }

    protected void MPI_Scatter(int []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, int []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionInt(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionInt(_world, dData, dCount, false);
        doScatter(sConn, dConn, rootID, comm);
    }

    protected void MPI_Scatter(double []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, double []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionDouble(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionDouble(_world, dData, dCount, false);
        doScatter(sConn, dConn, rootID, comm);
    }

    protected void MPI_Scatter(String []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, String []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionString(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionString(_world, dData, dCount, false);
        doScatter(sConn, dConn, rootID, comm);
    }

    ////////////////////////////////////////////////////////////////////
    // The general MPI_Gather function
    private void doGather(MPI_DataConnection sConn, MPI_DataConnection.MPI_Datatype sDatatype, MPI_DataConnection dConn, MPI_DataConnection.MPI_Datatype dDatatype, int rootID, int comm)
    {
        int actualLength = dConn.actualLength();
        int neededLength = _world.numProcs() * dConn.count();
        if (actualLength < neededLength)
        {
            _world.error(2, "The Gather destination array should have size " + neededLength + " but has size " + actualLength);
        }
        _state = MPI_ProcState.BLOCKED;
        _collectiveConn = sConn;
        _collectiveConn2 = dConn;
        startCollective(MPI_Collective.MPI_CollectiveType.GATHER, MPI_ReduceOp.SUM, _world.findProc(rootID));
        _collectiveConn = null;
        _collectiveConn2 = null;
        _state = MPI_ProcState.RUNNING;
    }

    protected void MPI_Gather(int []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, int []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionInt(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionInt(_world, dData, dCount, false);
        doGather(sConn, sDatatype, dConn, dDatatype, rootID, comm);
    }

    protected void MPI_Gather(double []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, double []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionDouble(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionDouble(_world, dData, dCount, false);
        doGather(sConn, sDatatype, dConn, dDatatype, rootID, comm);
    }

    protected void MPI_Gather(String []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, String []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionString(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionString(_world, dData, dCount, false);
        doGather(sConn, sDatatype, dConn, dDatatype, rootID, comm);
    }

    ////////////////////////////////////////////////////////////////////
    // The general MPI_Allgather function
    private void doAllgather(MPI_DataConnection sConn, MPI_DataConnection.MPI_Datatype sDatatype, MPI_DataConnection dConn, MPI_DataConnection.MPI_Datatype dDatatype, int comm)
    {
        int actualLength = dConn.actualLength();
        int neededLength = _world.numProcs() * dConn.count();
        if (actualLength < neededLength)
        {
            _world.error(2, "The Allgather destination array should have size " + neededLength + " but has size " + actualLength);
        }
        _state = MPI_ProcState.BLOCKED;
        _collectiveConn = sConn;
        _collectiveConn2 = dConn;
        startCollective(MPI_Collective.MPI_CollectiveType.GATHERALL, MPI_ReduceOp.SUM, null);
        _collectiveConn = null;
        _collectiveConn2 = null;
        _state = MPI_ProcState.RUNNING;
    }

    protected void MPI_Allgather(int []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, int []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionInt(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionInt(_world, dData, dCount, false);
        doAllgather(sConn, sDatatype, dConn, dDatatype, comm);
    }

    protected void MPI_Allgather(double []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, double []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionDouble(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionDouble(_world, dData, dCount, false);
        doAllgather(sConn, sDatatype, dConn, dDatatype, comm);
    }

    protected void MPI_Allgather(String []sData, int sCount, MPI_DataConnection.MPI_Datatype sDatatype, String []dData, int dCount, MPI_DataConnection.MPI_Datatype dDatatype, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionString(_world, sData, sCount, false);
        MPI_DataConnection dConn = new MPI_DataConnectionString(_world, dData, dCount, false);
        doAllgather(sConn, sDatatype, dConn, dDatatype, comm);
    }

    ////////////////////////////////////////////////////////////////////
    // The general MPI_Reduce function
    private void doReduce(MPI_DataConnection sConn, MPI_DataConnection dConn, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int rootID, int comm)
    {
        _state = MPI_ProcState.BLOCKED;
        _collectiveConn = sConn;
        _collectiveConn2 = dConn;
        startCollective(MPI_Collective.MPI_CollectiveType.REDUCE, op, _world.findProc(rootID));
        _collectiveConn = null;
        _collectiveConn2 = null;
        _state = MPI_ProcState.RUNNING;
    }

    protected void MPI_Reduce(int []sData, int []dData, int count, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionInt(_world, sData, count, false);
        MPI_DataConnection dConn = new MPI_DataConnectionInt(_world, dData, count, false);
        doReduce(sConn, dConn, datatype, op, rootID, comm);
    }

    protected void MPI_Reduce(double []sData, double []dData, int count, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionDouble(_world, sData, count, false);
        MPI_DataConnection dConn = new MPI_DataConnectionDouble(_world, dData, count, false);
        doReduce(sConn, dConn, datatype, op, rootID, comm);
    }

    protected void MPI_Reduce(String []sData, String []dData, int count, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int rootID, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionString(_world, sData, count, false);
        MPI_DataConnection dConn = new MPI_DataConnectionString(_world, dData, count, false);
        doReduce(sConn, dConn, datatype, op, rootID, comm);
    }

    ////////////////////////////////////////////////////////////////////
    // The general MPI_Allreduce function
    private void doAllreduce(MPI_DataConnection sConn, MPI_DataConnection dConn, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int comm)
    {
        _state = MPI_ProcState.BLOCKED;
        _collectiveConn = sConn;
        _collectiveConn2 = dConn;
        startCollective(MPI_Collective.MPI_CollectiveType.REDUCEALL, op, null);
        _collectiveConn = null;
        _collectiveConn2 = null;
        _state = MPI_ProcState.RUNNING;
    }

    protected void MPI_Allreduce(int []sData, int []dData, int count, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionInt(_world, sData, count, false);
        MPI_DataConnection dConn = new MPI_DataConnectionInt(_world, dData, count, false);
        doAllreduce(sConn, dConn, datatype, op, comm);
    }

    protected void MPI_Allreduce(double []sData, double []dData, int count, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionDouble(_world, sData, count, false);
        MPI_DataConnection dConn = new MPI_DataConnectionDouble(_world, dData, count, false);
        doAllreduce(sConn, dConn, datatype, op, comm);
    }

    protected void MPI_Allreduce(String []sData, String []dData, int count, MPI_DataConnection.MPI_Datatype datatype, MPI_ReduceOp op, int comm)
    {
        MPI_DataConnection sConn = new MPI_DataConnectionString(_world, sData, count, false);
        MPI_DataConnection dConn = new MPI_DataConnectionString(_world, dData, count, false);
        doAllreduce(sConn, dConn, datatype, op, comm);
    }

    ////////////////////////////////////////////////////////////////////
    // Start a collective
    private void startCollective(MPI_Collective.MPI_CollectiveType type, MPI_ReduceOp op, MPI_Proc root)
    {
        synchronized(_world)
        {
            // See if anybody is blocking for me, which would be an error
            int len = _world.numProcs();
            for (int i = 0 ; i < len; i++)
            {
                MPI_Proc proc = _world.findProc(i);
                if (proc._imBlockedForProc == this)
                {
                    _world.error(4, "Process " + _rank + " cannot enter " + MPI_Collective.typeAsString(type) + " because process "
                        + proc._rank + " is blocked on it");
                    return;
                }
            }

            // There can only be one collective at a time.  See if there is one already
            MPI_Collective collective = _world.collective();
            if (collective != null)
            {
                if (collective.type() != type)
                {
                    _world.error(4, "Process " + _rank + " cannot start " + MPI_Collective.typeAsString(type) + " because there is already a "
                            + MPI_Collective.typeAsString(collective.type()) + " started");
                    return;
                }
            }
            else
            {
                collective = new MPI_Collective(_world, type, op, root);
                _world.setCollective(collective);
            }

            // Join in to the collective.  If this returns 'true', the collective was full and has fired.
            if (collective.joinIn())
            {
                // The collective fired, so return
                _world.notifyAll();
                return;
            }

            // The collective is not yet full, so wait for it.
            _imBlockedForCollective = collective;
            while (collective.done() == false)
            {
                try
                {
                    _world.wait();
                }
                catch (InterruptedException e)
                {
                    // Empty
                }
            }
            _imBlockedForCollective = null;
        }
    }

    // Return a pending message with the given source, tag, and datatype.  Actually,
    // if the source and tag match, this is the correct message, but if the datatype
    // is wrong, we report the error.
    //
    // If this finds the message, it removes the message from the linked list.
    private MPI_PendingMessage getDstMessage(int sourceID, int tag, MPI_DataConnection.MPI_Datatype datatype)
    {
        MPI_PendingMessage msg;
        for (msg = _messages; msg != null; msg = msg.next())
        {
            if (sourceID != MPI_ANY_SOURCE && msg.from().rank() != sourceID)
            {
                continue;
            }
            if (tag != MPI_ANY_TAG && msg.tag() != tag)
            {
                continue;
            }
            if (msg.datatype() != datatype) // return either source or dest datatype
            {
                continue;
            }
            return msg;
        }
        return null;
    }

    // We have finished sending a message, so remove it from the list
    public void removeMessage(MPI_PendingMessage msg)
    {
        if (_messages == msg)
        {
            _messages = msg.next();
            return;
        }
        for (MPI_PendingMessage q = _messages; q != null; q = q.next())
        {
            if (q.next() == msg)
            {
                q.removeNext();
                return;
            }
        }
    }

    // Create a new pending message for this destination
    private MPI_PendingMessage createNewMessage(MPI_Proc source, int tag)
    {
        MPI_PendingMessage msg = new MPI_PendingMessage(source, this, tag, _messages);
        _messages = msg;
        return(msg);
    }

    ////////////////////////////////////////////////////////////////////
    // Print the status of this processor
    public void status(boolean sendToErr)
    {
        MPI_Proc other = _imBlockedForProc;

        String msg = "  Process " + _rank + ": state " + ProcStateToString(_state);
        if (_imBlockedForProc != null)
        {
            msg += ", blocked for " + _imBlockedForProc._rank;
        }
        if (sendToErr)
        {
            _world.error(-1, msg);
        }
        else
        {
            System.out.println(msg);
        }

        for (MPI_PendingMessage pnd = _messages; pnd != null; pnd = pnd.next())
        {
            pnd.status(sendToErr, _world);
        }
    }

    //------------------------------------------------------------------
    // Routines for the QV_View package
    //------------------------------------------------------------------

    public void QV_CreateView(int x, int y, int wid, int hgt)
    {
        if (_myView == null)
        {
            _myView = new QV_View(_rank, x, y, wid, hgt);
        }
    }

    public void QV_DestroyView()
    {
        _myView = null;
    }

    public void QV_ViewBeginScene()
    {
        if (_myView != null)
        {
            _myView.beginView();
        }
    }

    public void QV_ViewEndScene()
    {
        if (_myView != null)
        {
            _myView.endView();
        }
    }

    public void QV_ViewSceneBox(Color color, double x0, double y0, double x1, double y1)
    {
        if (_myView != null)
        {
            _myView.box(color, x0, y0, x1, y1);
        }
    }

    public void QV_ViewSceneLine(Color color, double x0, double y0, double x1, double y1)
    {
        if (_myView != null)
        {
            _myView.line(color, x0, y0, x1, y1);
        }
    }

    public void QV_ViewSceneText(Color color, double x, double y, double size, String msg)
    {
        if (_myView != null)
        {
            _myView.text(color, x, y, size, msg);
        }
    }

    public void QV_ViewDispose()
    {
        if (_myView != null)
        {
            _myView.dispose();
            _myView = null;
        }
    }
}
