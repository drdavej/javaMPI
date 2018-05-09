import java.awt.Color;

public class MPI_SampleProc extends MPI_Proc
{
    public MPI_SampleProc(MPI_World world, int rank)
    {
        super(world, rank);
    }

    public void exec(int argc, String argv[]) throws InterruptedException
    {
        MPI_Init(argc, argv);
        MPI_Status status = new MPI_Status();

        int data1[] = new int[4];
        int data2[] = new int[4];

        int rank = MPI_Comm_rank(MPI_COMM_WORLD);
        int size = MPI_Comm_size(MPI_COMM_WORLD);
        data1[0] = 213 * rank + 524;
        if (rank == 0)
        {
            QV_CreateView(10, 10, 300, 200);
            QV_ViewBeginScene();
            QV_ViewSceneBox(Color.BLACK, 0.25, 0.25, 0.66, 0.66);
            QV_ViewEndScene();
        }
        if (rank == 1)
        {
            QV_CreateView(310, 10, 300, 200);
            QV_ViewBeginScene();
            QV_ViewSceneText(Color.BLACK, 0.3, 0.4,  0.16, "Hi");
            QV_ViewEndScene();
        }
        if (rank > 0)
        {
            MPI_Recv(data2, 1, MPI_INT, rank - 1, 0, MPI_COMM_WORLD, status);
            System.out.println("Node " + rank + " received " + data2[0]);
        }
        if (rank < size - 1)
        {
            MPI_Send(data1, 1, MPI_INT, rank + 1, 0, MPI_COMM_WORLD);
        }

        MPI_Barrier();

        if (rank == 0)
        {
            data1[0] = 7132;
            data1[1] = 4353;
        }
        MPI_Bcast(data1, 2, MPI_INT, 0, MPI_COMM_WORLD);
        if (rank != 0)
        {
            System.out.println("BCast: Process " + rank + " received " + data1[0] + ", " + data1[1]);
        }

        MPI_Allreduce(data2, data1, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
        System.out.println("Reduce for " + rank + " returned " + data1[0]);

        data1[0] = 111;
        data1[1] = 222;
        data1[2] = 333;
        data1[3] = 444;
        MPI_Scatter(data1, 1, MPI_INT, data2, 1, MPI_INT, 0, MPI_COMM_WORLD);
        System.out.println("Scatter for " + rank + " returned " + data2[0]);

        data1[0] = rank;
        MPI_Allgather(data1, 1, MPI_INT, data2, 1, MPI_INT, MPI_COMM_WORLD);
        System.out.println("Allgather for " + rank + " returned " + data2[0] + " " + data2[1] + " " + data2[2] + " " + data2[3]);

        Thread.sleep(2000);

        MPI_Finalize();
        if (rank < 2)
        {
            QV_ViewDispose();
        }
    }
}
