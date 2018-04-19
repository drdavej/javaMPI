// To Do List:
// Make MPI_ANY_SOURCE, MPI_ANY_TAG, etc
// What do the MPI routines return (an integer)?
// If there is a tag mismatch, don't show as a deadlock, but as a suspected tag mismatch
// probes
// Good way to terminate program
import java.util.Scanner;

public class Main
{

    public static void main(String[] args)
    {
        Scanner reader = new Scanner(System.in);

        System.out.println("How many processors? ");
        int num = reader.nextInt();

        MPI_World world = new MPI_World(num);

        while (true)
        {
            System.out.println("Yes? ");
            num = reader.nextInt();
            if (num <= 0)
            {
                break;
            }
            if (num == 1)
            {
                world.status();
            }
        }
    }
}
