public class MPI_DataConnectionString extends MPI_DataConnection
{
    private String _data[];

    public MPI_DataConnectionString(MPI_World world, String data[], int count, boolean buffer)
    {
        super(world, count, MPI_DataConnection.MPI_Datatype.MPI_DT_STRING);
        if (data.length < count)
        {
            world.error(4, "Data connection asks for length " + count + " but data length is " + data.length);
        }
        if (buffer)
        {
            int len = data.length;
            _data = new String[len];
            for (int i = 0 ; i < len ; i++)
            {
                _data[i] = data[i];
            }
        }
        else
        {
            _data = data;
        }
    }


    public MPI_DataConnectionString asString()
    {
        return this;
    }

    public void transferFrom(MPI_DataConnection source, int srcOffset, int dstOffset)
    {
        MPI_DataConnectionString src = source.asString();
        if (src == null)
        {
            _world.error("Data transfer: wanted " + MPI_DataConnection.datatypeString(_datatype)
                    + " but got " + MPI_DataConnection.datatypeString(source.datatype()));
            return;
        }
        for (int i = 0 ; i < _count ; i++)
        {
            _data[dstOffset + i] = src._data[srcOffset + i];
        }
    }

    public void transferAll(MPI_DataConnection source, int len)
    {
        MPI_DataConnectionString src = source.asString();
        if (src == null)
        {
            _world.error("Data transfer: wanted " + MPI_DataConnection.datatypeString(_datatype)
                    + " but got " + MPI_DataConnection.datatypeString(source.datatype()));
            return;
        }
        for (int i = 0 ; i < len ; i++)
        {
            _data[i] = src._data[i];
        }
    }

    public void reduceFrom(MPI_DataConnection source, int rank, MPI_Proc.MPI_ReduceOp op)
    {
        // TBD -- error
    }

    public int actualLength()
    {
        return _data.length;
    }
}
