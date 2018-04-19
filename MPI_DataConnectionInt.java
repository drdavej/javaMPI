public class MPI_DataConnectionInt extends MPI_DataConnection
{
    private int _data[];

    public MPI_DataConnectionInt(MPI_World world, int data[], int count, boolean buffer)
    {
        super(world, count, MPI_DataConnection.MPI_Datatype.MPI_DT_INT);
        if (data.length < count)
        {
            world.error(4, "Data connection asks for length " + count + " but data length is " + data.length);
        }
        if (buffer)
        {
            int len = data.length;
            _data = new int[len];
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

    public MPI_DataConnectionInt asInt()
    {
        return this;
    }

    public void transferFrom(MPI_DataConnection source, int srcOffset, int dstOffset)
    {
        MPI_DataConnectionInt src = source.asInt();
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
        MPI_DataConnectionInt src = source.asInt();
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
        MPI_DataConnectionInt src = source.asInt();
        if (src == null)
        {
            _world.error("Data transfer: wanted " + MPI_DataConnection.datatypeString(_datatype)
                    + " but got " + MPI_DataConnection.datatypeString(source.datatype()));
            return;
        }

        // If the rank is 0, this is the first processor, so we just copy the values.
        // For all the others we do the 'reduce op'.
        if (rank == 0)
        {
            for (int i = 0 ; i < _count ; i++)
            {
                _data[i] = src._data[i];
            }
            if ((op == MPI_Proc.MPI_ReduceOp.MAXLOC || op == MPI_Proc.MPI_ReduceOp.MINLOC) && _count > 1)
            {
                _data[1] = 0;
            }
        }
        else
        {
            for (int i = 0 ; i < _count ; i++)
            {
                int a = _data[i];
                int b = src._data[i];

                switch (op)
                {
                case MAX:
                    _data[i] = (a > b) ? a : b;
                    break;
                case MIN:
                    _data[i] = (a < b) ? a : b;
                    break;
                case SUM:
                    _data[i] = a + b;
                    break;
                case PROD:
                    _data[i] = a * b;
                    break;
                case LAND:
                    _data[i] = (a != 0 && b != 0) ? 1 : 0;
                    break;
                case LOR:
                    _data[i] = (a != 0 || b != 0) ? 1 : 0;
                    break;
                case BAND:
                    _data[i] = a & b;
                    break;
                case BOR:
                    _data[i] = a | b;
                    break;
                case MAXLOC:
                    if (i == 0 && _count > 1)
                    {
                        if (b > a)
                        {
                            _data[i] = b;
                            _data[1] = rank;
                        }
                    }
                    else if (i != 1)
                    {
                        _data[i] = (a > b) ? a : b;
                    }
                    break;
                case MINLOC:
                    if (i == 0 && _count > 1)
                    {
                        if (b < a)
                        {
                            _data[i] = b;
                            _data[1] = rank;
                        }
                    }
                    else if (i != 1)
                    {
                        _data[i] = (a < b) ? a : b;
                    }
                    break;
                }
            }
        }
    }

    public int actualLength()
    {
        return _data.length;
    }
}
