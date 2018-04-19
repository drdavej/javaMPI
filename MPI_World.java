public class MPI_World
{
    int _numProcs;
    MPI_Proc _processes[];

    // If there is a collective going on, it will be stored here:
    MPI_Collective _collective;

    public MPI_World(int numProcs)
    {
        _numProcs = numProcs;
        _processes = new MPI_Proc[numProcs];
        _collective = null;
        for (int i = 0 ; i < numProcs; i++)
        {
            _processes[i] = MPI_ProcFactory.getProc(this, i);
        }
        for (int i = 0 ; i < numProcs; i++)
        {
            _processes[i].start();
        }
    }

    public void error(int depth, String message)
    {
        if (depth < 0)
        {
            System.out.println(message);
        }
        else
        {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[depth];
            String where = ste.getClassName() + "." + ste.getMethodName() + "() line " + ste.getLineNumber() + ": ";
            System.out.println(where + message);
        }
    }

    public void error(String message)
    {
        error(2, message);
    }

    public MPI_Proc findProc(int ID)
    {
        if (ID < 0 || ID >= _numProcs)
        {
            return null;
        }
        return _processes[ID];
    }

    public int numProcs() { return _numProcs; }

    public void status()
    {
        for (int i = 0 ; i < _numProcs; i++)
        {
            _processes[i].status(false);
        }
    }

    public void setCollective(MPI_Collective collective) { _collective = collective; }
    public MPI_Collective collective() { return _collective; }
}
