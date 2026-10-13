package rearth.oritech.port.neoforge.transfer.resource;

public record ResourceStack<T extends Resource>(T resource, int amount) {
    public boolean isEmpty() {
        return resource.isEmpty() || amount <= 0;
    }
}
