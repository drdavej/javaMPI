// An MPI_DataConnection holds the source or destination for a message.
// It is subclassed to hold connections for integer, float, or string data.
// The subclass has the actual pointer.

public class MPI_DataConnection
{
    // Reference to the world (so we can send error messages)
    MPI_World _world;

    public enum MPI_Datatype { MPI_DT_INT, MPI_DT_DOUBLE, MPI_DT_STRING };

    // The datatype for this connection, one of the above three values
    protected MPI_Datatype _datatype;

    // The number of entries being transferred
    protected int _count;

    public MPI_DataConnection(MPI_World world, int count, MPI_Datatype datatype)
    {
        _world = world;
        _datatype = datatype;
        _count = count;
    }

    public MPI_Datatype datatype() { return _datatype; }
    public int count() { return _count; }

    static public String datatypeString(MPI_Datatype datatype)
    {
        switch (datatype)
        {
        case MPI_DT_INT:    return "MPI_INT";
        case MPI_DT_DOUBLE: return "MPI_DOUBLE";
        case MPI_DT_STRING: return "MPI_STRING";
        }
        return "Unknown Datatype";
    }

    // These are overwritten

    public MPI_DataConnectionInt asInt()
    {
        return null;
    }

    public MPI_DataConnectionDouble asDouble()
    {
        return null;
    }

    public MPI_DataConnectionString asString()
    {
        return null;
    }

    // These are overwritten
    public void transferFrom(MPI_DataConnection source, int srcOffset, int dstOffset)
    {
    }

    public void transferAll(MPI_DataConnection source, int len)
    {
    }

    public void reduceFrom(MPI_DataConnection source, int rank, MPI_Proc.MPI_ReduceOp op)
    {

    }

    public int actualLength()
    {
        return 0;
    }
}
