export class DelayedTask {
    private running: boolean = false;
    private delayed: boolean = false;
    private maxDelay: number = 0;
    private intervalHandle: number | null = null;

    constructor(
        private fn: Function,
        private fnScope: any,
        private fnArgs?: any[]
    ) {}

    private async call(): Promise<void> {
        this.running = true;
        this.delayed = false;
        this.cancel();
        await this.fn.apply(this.fnScope, this.fnArgs || []);
        this.running = false;
        if (this.delayed) {
            this.delay(this.maxDelay);
            this.maxDelay = 0;
        }
    }

    public delay(delay: number): void {
        if (this.running) {
            this.delayed = true;
            this.maxDelay = Math.max(this.maxDelay, delay);
        } else {
            this.cancel();
            this.intervalHandle = setInterval(this.call.bind(this), delay);
        }
    }

    public cancel(): void {
        if (this.intervalHandle) {
            clearInterval(this.intervalHandle);
            this.intervalHandle = null;
        }
    }
}