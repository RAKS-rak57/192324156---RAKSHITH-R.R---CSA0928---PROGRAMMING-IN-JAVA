CREATE INDEX idx_pass_reg   ON bus_pass_full(reg_no);       -- "find the passes of a student"
CREATE INDEX idx_pass_route ON bus_pass_full(route_no);     -- "list the passes of a route"
