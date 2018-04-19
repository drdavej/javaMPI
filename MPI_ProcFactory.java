public class MPI_ProcFactory
{
    public static MPI_Proc getProc(MPI_World world, int rank)
    {
        // TBD -- This code should call your class' constructor
        MPI_Proc proc = new MPI_SampleProc(world, rank);
        Thread thread = new Thread(proc);
        proc.setThread(thread);
        return proc;
    }
}
