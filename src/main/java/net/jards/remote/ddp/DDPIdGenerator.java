package net.jards.remote.ddp;

import net.jards.core.IdGenerator;

import java.util.UUID;

/**
 * Created by jDzama on 24.1.2017.
 */
public class DDPIdGenerator implements IdGenerator{

    private final String seed;

    public DDPIdGenerator(String seed){
        this.seed = seed;
    }

    @Override
    public UUID getId() {
        return UUID.randomUUID();
    }
}
