package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.service.IAddressService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final IAddressService addressService;

    @Autowired
    public AddressController(IAddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<Address> create(@RequestBody Address address) {
        Address created = addressService.create(address);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Address> read(@PathVariable UUID id) {
        Address address = addressService.read(id);
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Address> update(@PathVariable UUID id, @RequestBody Address address) {
        Address existing = addressService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Address toUpdate = new Address.Builder().copy(address).setId(id).build();
        return ResponseEntity.ok(addressService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!addressService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Address>> getAll() {
        return ResponseEntity.ok(addressService.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Address>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(addressService.getByUser(userId));
    }

    @GetMapping("/user/{userId}/default")
    public ResponseEntity<Address> getDefault(@PathVariable UUID userId) {
        Address address = addressService.getDefaultForUser(userId);
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<Address> setDefault(@PathVariable UUID id) {
        Address address = addressService.setDefault(id);
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }
}