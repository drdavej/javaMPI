public class MPI_Status
{
    public int MPI_SOURCE;
    public int MPI_TAG;
    public int MPI_COUNT;

    MPI_Status()
    {
        MPI_SOURCE = -1;
        MPI_TAG = -1;
        MPI_COUNT = 0;
    }
}
