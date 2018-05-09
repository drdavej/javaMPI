// Collectives:
//  Init
//  Finalize
//  Barrier
//  Bcast
//  Reduce
//  Scatter
//  Gather
//  ReduceAll
//  GatherAll
//
// The world can have at most one collective at a time.
// When the collective is created, it is added to the world.
// As each proc joins the collective, it adds to the list of
// who is there, or at least it updates the count.
// When the last proc joins the collective, it fires, does its
// work, and releases all of the procs.
//
// If there is already a collective, an error message is generated.
// If the only processes not in the collective are blocked, we have
// a problem, so check for this.
// More interesting: if proc A is blocked waiting for proc B, and
// proc B joins a collective, report the error, as B will become blocking,
// so it can never release A, and A can never join the collective.

public class MPI_Collective
{
    public enum MPI_CollectiveType { INIT, FINALIZE, BARRIER, BCAST, REDUCE, SCATTER, GATHER, REDUCEALL, GATHERALL };

    MPI_CollectiveType _type;

    MPI_World _world;

    // How many have yet to join in:
    int _numLeft;

    // Optional:
    MPI_Proc _root;
    MPI_Proc.MPI_ReduceOp _op;

    MPI_Collective(MPI_World world, MPI_CollectiveType type, MPI_Proc.MPI_ReduceOp op, MPI_Proc root)
    {
        _world = world;
        _type = type;
        _numLeft = world.numProcs();
        _op = op;
        _root = root;
    }

    public MPI_CollectiveType type() { return _type; }

    static public String typeAsString(MPI_CollectiveType type)
    {
        switch (type)
        {
        case INIT:      return "INIT";
        case FINALIZE:  return "FINALIZE";
        case BARRIER:   return "BARRIER";
        case BCAST:     return "BCAST";
        case REDUCE:    return "REDUCE";
        case SCATTER:   return "SCATTER";
        case GATHER:    return "GATHER";
        case REDUCEALL: return "REDUCEALL";
        case GATHERALL: return "GATHERALL";
        }
        return "UNKNOWN";
    }

    private void doBcast()
    {
        int len = _world.numProcs();
        for (int i = 0 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            if (proc != _root)
            {
                proc.collectiveConn().transferFrom(_root.collectiveConn(), 0, 0);
            }
        }
    }

    private void doReduce()
    {
        int len = _world.numProcs();
        MPI_DataConnection dest = _root.collectiveConn2();
        for (int i = 0 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            dest.reduceFrom(proc.collectiveConn(), i, _op);
        }
    }

    private void doAllreduce()
    {
        int len = _world.numProcs();
        MPI_DataConnection dest = _world.findProc(0).collectiveConn2();
        for (int i = 0 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            dest.reduceFrom(proc.collectiveConn(), i, _op);
        }
        for (int i = 1 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            proc.collectiveConn2().transferFrom(dest, 0, 0);
        }
    }

    private void doScatter()
    {
        int len = _world.numProcs();
        MPI_DataConnection src = _root.collectiveConn();
        int step = src.count();
        for (int i = 0 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            proc.collectiveConn2().transferFrom(src, i * step, 0);
        }
    }

    private void doGather()
    {
        int len = _world.numProcs();
        MPI_DataConnection dst = _root.collectiveConn2();
        int step = dst.count();
        for (int i = 0 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            dst.transferFrom(proc.collectiveConn(), 0, i * step);
        }
    }

    private void doAllgather()
    {
        int len = _world.numProcs();
        MPI_DataConnection dst = _world.findProc(0).collectiveConn2();
        int step = dst.count();
        for (int i = 0 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            dst.transferFrom(proc.collectiveConn(), 0, i * step);
        }
        for (int i = 1 ; i < len; i++)
        {
            MPI_Proc proc = _world.findProc(i);
            MPI_DataConnection cpy = proc.collectiveConn2();
            cpy.transferAll(dst, len * step);
        }
    }

    public boolean joinIn()
    {
        _numLeft--;

        if (_numLeft == 0)
        {
            // Remove from world
            _world.setCollective(null);

            // Remove all processes from the collective (so when any of them
            // start running, all of the processes aren't still blocked)
            int len = _world.numProcs();
            for (int i = 0 ; i < len; i++)
            {
                MPI_Proc proc = _world.findProc(i);
                proc.setImBlockedForCollective(null);
            }

            // Run the logic of the collective
            switch (_type)
            {
            case INIT:
            case FINALIZE:
            case BARRIER:
                // EMPTY
                break;
            case BCAST:
                doBcast();
                break;
            case REDUCE:
                doReduce();
                break;
            case SCATTER:
                doScatter();
                break;
            case GATHER:
                doGather();
                break;
            case REDUCEALL:
                doAllreduce();
                break;
            case GATHERALL:
                doAllgather();
                break;
            }
            return true;
        }
        return false;
    }

    public boolean done() { return _numLeft == 0; }
}
