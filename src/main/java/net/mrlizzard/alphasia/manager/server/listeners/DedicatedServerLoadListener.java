package net.mrlizzard.alphasia.manager.server.listeners;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.PubSubConsumer;
import org.apache.log4j.Level;

public class DedicatedServerLoadListener implements PubSubConsumer {

    @Override
    public void consume(String channel, String message) {
        if(message.startsWith("load")) {
            try {
                String[] splitter = message.split(" ");
                String address = splitter[1];
                double cpu = Double.parseDouble(splitter[2]);
                long freeRAM = Long.parseLong(splitter[3]);

                AlphaManagerServer.log(Level.INFO, address + " load => CPU: " + cpu + " | RAM: " + freeRAM);

                /*if (ClientEntity.getClientWithAddress(address) == null)
                    return;

                ClientEntity clientEntity = ClientEntity.getClientWithAddress(address);
                clientEntity.keep(cpu, freeRAM);*/
            } catch (Exception ingored) {}
        }
    }

}
