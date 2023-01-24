import java.util.concurrent.Semaphore;

public class ReaderWriter
{
    int numReaders = 0;
    Semaphore mutex = new Semaphore(1);
    Semaphore lock = new Semaphore(1);
    public void startRead()
    {
        try
        {
            mutex.acquire();
            numReaders++;
            if (numReaders == 1)
                lock.acquire();
            mutex.release();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    public void endRead()
    {
        try
        {
            mutex.acquire();
            numReaders--;
            if (numReaders == 0)
                lock.release();
            mutex.release();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    public void startWrite()
    {
        try
        {
            lock.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    public void endWrite()
    {
        lock.release();
    }
}